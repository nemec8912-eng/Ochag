from fastapi import FastAPI, WebSocket, WebSocketDisconnect, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from pathlib import Path

import json
import sqlite3
import time
import uuid
import hashlib
import hmac
import secrets


APP_VERSION = "2.9.0"

app = FastAPI(
    title="Ochag Server",
    version=APP_VERSION
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"]
)


UPLOAD_DIR = Path("uploads")
UPLOAD_DIR.mkdir(
    parents=True,
    exist_ok=True
)

DATABASE_PATH = Path(
    "ochag_server.db"
)

app.mount(
    "/uploads",
    StaticFiles(
        directory=str(
            UPLOAD_DIR
        )
    ),
    name="uploads"
)


connected_clients: dict[str, WebSocket] = {}


def db_connect():
    connection = sqlite3.connect(
        DATABASE_PATH
    )
    connection.row_factory = sqlite3.Row
    return connection


def init_database():

    with db_connect() as db:

        db.execute(
            """
            CREATE TABLE IF NOT EXISTS chats (
                chat_id INTEGER PRIMARY KEY,
                title TEXT NOT NULL DEFAULT '',
                chat_type TEXT NOT NULL DEFAULT 'PRIVATE',
                members_json TEXT NOT NULL DEFAULT '[]',
                updated_at INTEGER NOT NULL DEFAULT 0
            )
            """
        )

        db.execute(
            """
            CREATE TABLE IF NOT EXISTS messages (
                message_id INTEGER PRIMARY KEY,
                chat_id INTEGER NOT NULL,
                sender_name TEXT NOT NULL,
                message_type TEXT NOT NULL,
                text TEXT NOT NULL DEFAULT '',
                media_url TEXT NOT NULL DEFAULT '',
                file_name TEXT NOT NULL DEFAULT '',
                caption TEXT NOT NULL DEFAULT '',
                members_json TEXT NOT NULL DEFAULT '[]',
                created_at INTEGER NOT NULL
            )
            """
        )

        db.execute(
            """
            CREATE TABLE IF NOT EXISTS receipts (
                message_id INTEGER NOT NULL,
                user_name TEXT NOT NULL,
                delivered INTEGER NOT NULL DEFAULT 0,
                read INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (
                    message_id,
                    user_name
                )
            )
            """
        )

        db.execute(
            """
            CREATE INDEX IF NOT EXISTS idx_messages_chat_created
            ON messages(
                chat_id,
                created_at
            )
            """
        )

        db.execute(
            """
            CREATE INDEX IF NOT EXISTS idx_receipts_user
            ON receipts(
                user_name,
                delivered,
                read
            )
            """
        )



        db.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                login TEXT NOT NULL UNIQUE COLLATE NOCASE,
                name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                pin_salt TEXT NOT NULL,
                pin_hash TEXT NOT NULL,
                is_owner INTEGER NOT NULL DEFAULT 0,
                family_id TEXT NOT NULL DEFAULT '',
                avatar_url TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """
        )

        db.execute(
            """
            CREATE TABLE IF NOT EXISTS join_requests (
                request_id INTEGER PRIMARY KEY AUTOINCREMENT,
                login TEXT NOT NULL UNIQUE COLLATE NOCASE,
                name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                pin_salt TEXT NOT NULL,
                pin_hash TEXT NOT NULL,
                invite_code TEXT NOT NULL DEFAULT '',
                family_id TEXT NOT NULL DEFAULT '',
                status TEXT NOT NULL DEFAULT 'pending',
                created_at INTEGER NOT NULL
            )
            """
        )

        db.execute(
            """
            CREATE TABLE IF NOT EXISTS chat_members (
                chat_id INTEGER NOT NULL,
                user_name TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'member',
                joined_at INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (chat_id, user_name)
            )
            """
        )

        db.execute(
            """
            CREATE INDEX IF NOT EXISTS idx_chat_members_user
            ON chat_members(user_name, chat_id)
            """
        )

        user_columns = {
            row["name"]
            for row in db.execute(
                "PRAGMA table_info(users)"
            ).fetchall()
        }

        if "phone" not in user_columns:
            db.execute(
                """
                ALTER TABLE users
                ADD COLUMN phone TEXT NOT NULL DEFAULT ''
                """
            )

        if "phone_normalized" not in user_columns:
            db.execute(
                """
                ALTER TABLE users
                ADD COLUMN phone_normalized TEXT NOT NULL DEFAULT ''
                """
            )

        db.execute(
            """
            CREATE INDEX IF NOT EXISTS idx_users_phone_normalized
            ON users(phone_normalized)
            """
        )
        db.commit()


init_database()



class RegisterRequest(BaseModel):
    login: str
    name: str
    pin: str
    phone: str = ""
    is_owner: bool = False
    family_id: str = ""


class ProfilePhoneRequest(BaseModel):
    user_name: str
    phone: str = ""


class JoinRequestCreate(BaseModel):
    login: str
    name: str
    pin: str
    invite_code: str = ""


class JoinRequestDecision(BaseModel):
    owner_name: str


class LoginRequest(BaseModel):
    login: str
    pin: str


class ChatUpsertRequest(BaseModel):
    chat_id: int = 0
    title: str = ""
    chat_type: str = "GROUP"
    members: list[str] = []


def now_ms():
    return int(time.time() * 1000)


def normalize_login(value: str):
    return str(value or "").strip().lower()


def normalize_phone(value: str):
    digits = "".join(
        ch
        for ch in str(value or "")
        if ch.isdigit()
    )

    if len(digits) == 11 and digits.startswith("8"):
        return "7" + digits[1:]

    if len(digits) == 10:
        return "7" + digits

    return digits


def validate_pin(pin: str):
    clean = str(pin or "").strip()
    if not clean.isdigit() or len(clean) < 4 or len(clean) > 12:
        raise HTTPException(
            status_code=400,
            detail="PIN должен содержать от 4 до 12 цифр"
        )
    return clean


def hash_pin(pin: str, salt_hex: str):
    return hashlib.pbkdf2_hmac(
        "sha256",
        pin.encode("utf-8"),
        bytes.fromhex(salt_hex),
        120_000
    ).hex()


def sync_chat_members(
    chat_id: int,
    members: list[str]
):
    clean = []
    for item in members:
        name = str(item or "").strip()
        if name and name not in clean:
            clean.append(name)

    with db_connect() as db:
        db.execute(
            "DELETE FROM chat_members WHERE chat_id = ?",
            (chat_id,)
        )

        db.executemany(
            """
            INSERT OR REPLACE INTO chat_members(
                chat_id,
                user_name,
                role,
                joined_at
            )
            VALUES (?, ?, 'member', ?)
            """,
            [
                (chat_id, member, now_ms())
                for member in clean
            ]
        )
        db.commit()


def split_glued_participant_name(value: str):
    clean = str(value or "").strip()

    if not clean:
        return []

    parts = []
    current = []

    for index, char in enumerate(clean):
        previous = clean[index - 1] if index > 0 else ""

        should_split = (
            bool(current)
            and char.isupper()
            and (
                previous.islower()
                or previous.isdigit()
            )
        )

        if should_split:
            part = "".join(current).strip(" ,;|/\\")
            if part:
                parts.append(part)
            current = []

        current.append(char)

    part = "".join(current).strip(" ,;|/\\")
    if part:
        parts.append(part)

    return parts if len(parts) > 1 else [clean]


def all_server_users():
    """
    Возвращает нормальный список участников для Android.

    Источники:
    1. зарегистрированные аккаунты из users;
    2. участники, уже встречающиеся в members_json существующих чатов.

    Это сохраняет совместимость со старой базой v1.9, где участники семьи
    могли существовать в чатах ещё до появления таблицы users.
    """

    registered = []

    with db_connect() as db:
        rows = db.execute(
            """
            SELECT user_id, login, name, is_owner, family_id, avatar_url,
                   phone, phone_normalized
            FROM users
            ORDER BY is_owner DESC, name COLLATE NOCASE ASC
            """
        ).fetchall()

        chat_rows = db.execute(
            """
            SELECT members_json
            FROM chats
            WHERE members_json IS NOT NULL
              AND members_json != ''
            """
        ).fetchall()

    for row in rows:
        name = str(row["name"] or "").strip()

        if not name:
            continue

        registered.append({
            "user_id": int(row["user_id"]),
            "login": row["login"],
            "name": name,
            "is_owner": bool(row["is_owner"]),
            "family_id": row["family_id"] or "",
            "avatar_url": row["avatar_url"] or "",
            "phone": row["phone"] or "",
            "phone_normalized": row["phone_normalized"] or ""
        })

    known_chat_names = []

    for row in chat_rows:
        try:
            members = json.loads(
                row["members_json"]
                or "[]"
            )
        except Exception:
            members = []

        if not isinstance(members, list):
            continue

        for item in members:
            for name in split_glued_participant_name(item):

                if (
                    name
                    and not any(
                        existing.casefold() ==
                        name.casefold()
                        for existing in known_chat_names
                    )
                ):
                    known_chat_names.append(name)

    def compact_name(value: str):
        return "".join(
            ch.casefold()
            for ch in str(value or "")
            if ch.isalnum()
        )

    # Защита от старой ошибки вида:
    # "МихаилАннаДочьСынБабушка"
    # Такой элемент не отдаём, если он явно состоит из нескольких
    # уже известных имён участников чатов.
    clean_registered = []

    expanded_registered = []

    for item in registered:
        split_names = split_glued_participant_name(
            item["name"]
        )

        if len(split_names) > 1:
            for split_name in split_names:
                clone = dict(item)
                clone["user_id"] = 0
                clone["login"] = ""
                clone["name"] = split_name
                clone["is_owner"] = False
                expanded_registered.append(clone)
        else:
            expanded_registered.append(item)

    registered = expanded_registered

    for item in registered:
        name = item["name"]
        compact = compact_name(name)

        matched_known = [
            known_name
            for known_name in known_chat_names
            if (
                compact_name(known_name)
                and compact_name(known_name) in compact
            )
        ]

        if (
            len(matched_known) >= 2
            and not any(
                known_name.casefold() ==
                name.casefold()
                for known_name in known_chat_names
            )
        ):
            continue

        clean_registered.append(item)

    result = list(clean_registered)

    for name in known_chat_names:
        if any(
            str(item.get("name", "")).casefold() ==
            name.casefold()
            for item in result
        ):
            continue

        result.append({
            "user_id": 0,
            "login": "",
            "name": name,
            "is_owner": False,
            "family_id": "",
            "avatar_url": "",
            "phone": "",
            "phone_normalized": ""
        })

    result.sort(
        key=lambda item: (
            0 if item.get("is_owner") else 1,
            str(item.get("name", "")).casefold()
        )
    )

    return result


def chats_for_user(user_name: str):
    result = []
    with db_connect() as db:
        rows = db.execute(
            """
            SELECT c.chat_id, c.title, c.chat_type, c.members_json, c.updated_at
            FROM chats c
            ORDER BY c.updated_at DESC, c.chat_id ASC
            """
        ).fetchall()

    for row in rows:
        try:
            members = json.loads(row["members_json"] or "[]")
        except Exception:
            members = []

        if user_name not in members:
            continue

        result.append({
            "chat_id": int(row["chat_id"]),
            "title": row["title"] or "",
            "chat_type": row["chat_type"] or "GROUP",
            "members": members,
            "updated_at": int(row["updated_at"] or 0)
        })

    return result


def clean_members(
    value,
    sender_name: str
):
    if not isinstance(
        value,
        list
    ):
        value = []

    result = []

    for item in value:

        name = str(
            item
        ).strip()

        if (
            name
            and name not in result
        ):
            result.append(
                name
            )

    if (
        sender_name
        and sender_name not in result
    ):
        result.append(
            sender_name
        )

    return result


def save_chat_metadata(
    chat_id: int,
    title: str,
    chat_type: str,
    members: list[str]
):

    with db_connect() as db:

        db.execute(
            """
            INSERT INTO chats(
                chat_id,
                title,
                chat_type,
                members_json,
                updated_at
            )
            VALUES(
                ?,
                ?,
                ?,
                ?,
                ?
            )
            ON CONFLICT(chat_id)
            DO UPDATE SET
                title = CASE
                    WHEN excluded.title != ''
                    THEN excluded.title
                    ELSE chats.title
                END,
                chat_type = CASE
                    WHEN excluded.chat_type != ''
                    THEN excluded.chat_type
                    ELSE chats.chat_type
                END,
                members_json = CASE
                    WHEN excluded.members_json != '[]'
                    THEN excluded.members_json
                    ELSE chats.members_json
                END,
                updated_at = excluded.updated_at
            """,
            (
                chat_id,
                title.strip(),
                chat_type.strip()
                    or "PRIVATE",
                json.dumps(
                    members,
                    ensure_ascii=False
                ),
                int(
                    time.time() *
                    1000
                )
            )
        )

        db.commit()

    if members:
        sync_chat_members(chat_id, members)


def save_message(
    packet: dict
):

    message_id = int(
        packet.get(
            "message_id"
        )
    )

    chat_id = int(
        packet.get(
            "chat_id"
        )
    )

    sender_name = str(
        packet.get(
            "sender_name",
            ""
        )
    ).strip()

    members = clean_members(
        packet.get(
            "members",
            []
        ),
        sender_name
    )

    created_at = int(
        packet.get(
            "created_at"
        )
        or int(
            time.time() *
            1000
        )
    )

    chat_title = str(
        packet.get(
            "chat_title",
            ""
        )
    )

    chat_type = str(
        packet.get(
            "chat_type",
            "PRIVATE"
        )
    )

    save_chat_metadata(
        chat_id = chat_id,
        title = chat_title,
        chat_type = chat_type,
        members = members
    )

    with db_connect() as db:

        db.execute(
            """
            INSERT OR IGNORE INTO messages(
                message_id,
                chat_id,
                sender_name,
                message_type,
                text,
                media_url,
                file_name,
                caption,
                members_json,
                created_at
            )
            VALUES(
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?
            )
            """,
            (
                message_id,
                chat_id,
                sender_name,
                str(
                    packet.get(
                        "media_type",
                        "TEXT"
                    )
                ),
                str(
                    packet.get(
                        "text",
                        ""
                    )
                ),
                str(
                    packet.get(
                        "media_url",
                        ""
                    )
                ),
                str(
                    packet.get(
                        "file_name",
                        ""
                    )
                ),
                str(
                    packet.get(
                        "caption",
                        ""
                    )
                ),
                json.dumps(
                    members,
                    ensure_ascii=False
                ),
                created_at
            )
        )

        for member in members:

            if (
                member == sender_name
            ):
                continue

            db.execute(
                """
                INSERT OR IGNORE INTO receipts(
                    message_id,
                    user_name,
                    delivered,
                    read
                )
                VALUES(
                    ?,
                    ?,
                    0,
                    0
                )
                """,
                (
                    message_id,
                    member
                )
            )

        db.commit()

    return created_at


def mark_delivered(
    message_id: int,
    user_name: str
):

    with db_connect() as db:

        db.execute(
            """
            UPDATE receipts
            SET delivered = 1
            WHERE message_id = ?
              AND user_name = ?
            """,
            (
                message_id,
                user_name
            )
        )

        db.commit()


def mark_read(
    message_ids: list[int],
    user_name: str
):

    if not message_ids:
        return

    with db_connect() as db:

        db.executemany(
            """
            UPDATE receipts
            SET delivered = 1,
                read = 1
            WHERE message_id = ?
              AND user_name = ?
            """,
            [
                (
                    message_id,
                    user_name
                )
                for message_id
                in message_ids
            ]
        )

        db.commit()


def aggregate_outgoing_status(
    message_id: int
):

    with db_connect() as db:

        rows = db.execute(
            """
            SELECT
                delivered,
                read
            FROM receipts
            WHERE message_id = ?
            """,
            (
                message_id,
            )
        ).fetchall()

    if not rows:
        return "SENT"

    if all(
        row["read"] == 1
        for row in rows
    ):
        return "READ"

    if all(
        row["delivered"] == 1
        for row in rows
    ):
        return "DELIVERED"

    return "SENT"


def history_for_user(
    user_name: str
):

    result = []

    with db_connect() as db:

        rows = db.execute(
            """
            SELECT
                m.*,
                c.title AS chat_title,
                c.chat_type AS chat_type
            FROM messages m
            LEFT JOIN chats c
              ON c.chat_id = m.chat_id
            ORDER BY
                m.created_at ASC,
                m.message_id ASC
            """
        ).fetchall()

        for row in rows:

            try:
                members = json.loads(
                    row["members_json"]
                    or "[]"
                )
            except Exception:
                members = []

            if (
                user_name not in members
                and row["sender_name"] != user_name
            ):
                continue

            if (
                row["sender_name"] ==
                user_name
            ):

                status = aggregate_outgoing_status(
                    row["message_id"]
                )

            else:

                receipt = db.execute(
                    """
                    SELECT
                        delivered,
                        read
                    FROM receipts
                    WHERE message_id = ?
                      AND user_name = ?
                    """,
                    (
                        row["message_id"],
                        user_name
                    )
                ).fetchone()

                if (
                    receipt is not None
                    and receipt["read"] == 1
                ):
                    status = "READ"

                else:
                    status = "DELIVERED"

            result.append(
                {
                    "message_id":
                        row["message_id"],

                    "chat_id":
                        row["chat_id"],

                    "sender_name":
                        row["sender_name"],

                    "media_type":
                        row["message_type"],

                    "text":
                        row["text"],

                    "media_url":
                        row["media_url"],

                    "file_name":
                        row["file_name"],

                    "caption":
                        row["caption"],

                    "members":
                        members,

                    "created_at":
                        row["created_at"],

                    "status":
                        status,

                    "chat_title":
                        row["chat_title"]
                        or "",

                    "chat_type":
                        row["chat_type"]
                        or "PRIVATE"
                }
            )

    return result


def pending_message_ids_for_user(
    user_name: str
):

    with db_connect() as db:

        rows = db.execute(
            """
            SELECT message_id
            FROM receipts
            WHERE user_name = ?
              AND delivered = 0
            """,
            (
                user_name,
            )
        ).fetchall()

    return [
        int(
            row["message_id"]
        )
        for row in rows
    ]


def sender_for_message(
    message_id: int
):

    with db_connect() as db:

        row = db.execute(
            """
            SELECT sender_name
            FROM messages
            WHERE message_id = ?
            """,
            (
                message_id,
            )
        ).fetchone()

    if row is None:
        return None

    return str(
        row["sender_name"]
    )


async def notify_sender_delivered(
    message_ids: list[int]
):

    by_sender: dict[str, list[int]] = {}

    for message_id in message_ids:

        sender = sender_for_message(
            message_id
        )

        if not sender:
            continue

        by_sender.setdefault(
            sender,
            []
        ).append(
            message_id
        )

    for sender, ids in by_sender.items():

        websocket = connected_clients.get(
            sender
        )

        if websocket is None:
            continue

        try:
            await websocket.send_json(
                {
                    "type":
                        "delivery_receipt",

                    "message_ids":
                        ids
                }
            )
        except Exception:
            pass


async def send_history_sync(
    user_name: str,
    websocket: WebSocket
):

    messages = history_for_user(
        user_name
    )

    await websocket.send_json(
        {
            "type":
                "history_sync",

            "messages":
                messages
        }
    )

    pending_ids = pending_message_ids_for_user(
        user_name
    )

    for message_id in pending_ids:
        mark_delivered(
            message_id,
            user_name
        )

    if pending_ids:

        await notify_sender_delivered(
            pending_ids
        )

    print(
        f"[SYNC] user={user_name} "
        f"history={len(messages)} "
        f"offline_delivered={len(pending_ids)}"
    )



@app.post("/family/join-requests")
async def create_join_request(payload: JoinRequestCreate):
    login = normalize_login(payload.login)
    name = str(payload.name or "").strip()
    pin = validate_pin(payload.pin)
    invite_code = str(payload.invite_code or "").strip().upper()

    if len(login) < 2 or len(name) < 2:
        raise HTTPException(status_code=400, detail="Проверьте имя и логин")

    with db_connect() as db:
        owner = db.execute(
            "SELECT family_id FROM users WHERE is_owner = 1 ORDER BY user_id LIMIT 1"
        ).fetchone()
        if owner is None:
            raise HTTPException(status_code=409, detail="Сначала владелец должен войти в Очаг")

        existing_user = db.execute(
            "SELECT 1 FROM users WHERE login = ? COLLATE NOCASE OR name = ? COLLATE NOCASE",
            (login, name)
        ).fetchone()
        if existing_user is not None:
            raise HTTPException(status_code=409, detail="Такой пользователь уже зарегистрирован")

        salt = secrets.token_hex(16)
        pin_hash = hash_pin(pin, salt)
        try:
            cursor = db.execute(
                """
                INSERT INTO join_requests(
                    login, name, pin_salt, pin_hash, invite_code,
                    family_id, status, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'pending', ?)
                """,
                (login, name, salt, pin_hash, invite_code, owner["family_id"] or "", now_ms())
            )
            db.commit()
        except sqlite3.IntegrityError:
            raise HTTPException(status_code=409, detail="Такая заявка уже отправлена")

    return {"status": "ok", "request_id": int(cursor.lastrowid)}


@app.get("/family/join-requests")
async def list_join_requests(owner_name: str):
    owner_name = str(owner_name or "").strip()
    with db_connect() as db:
        owner = db.execute(
            "SELECT family_id, is_owner FROM users WHERE name = ? COLLATE NOCASE",
            (owner_name,)
        ).fetchone()
        if owner is None or not bool(owner["is_owner"]):
            raise HTTPException(status_code=403, detail="Только владелец может смотреть заявки")
        rows = db.execute(
            """
            SELECT request_id, login, name, invite_code, created_at
            FROM join_requests
            WHERE family_id = ? AND status = 'pending'
            ORDER BY created_at
            """,
            (owner["family_id"] or "",)
        ).fetchall()

    return {"status": "ok", "requests": [dict(row) for row in rows]}


@app.post("/family/join-requests/{request_id}/approve")
async def approve_join_request(request_id: int, payload: JoinRequestDecision):
    owner_name = str(payload.owner_name or "").strip()
    with db_connect() as db:
        owner = db.execute(
            "SELECT family_id, is_owner FROM users WHERE name = ? COLLATE NOCASE",
            (owner_name,)
        ).fetchone()
        if owner is None or not bool(owner["is_owner"]):
            raise HTTPException(status_code=403, detail="Только владелец может подтверждать заявки")

        request_row = db.execute(
            """
            SELECT * FROM join_requests
            WHERE request_id = ? AND family_id = ? AND status = 'pending'
            """,
            (request_id, owner["family_id"] or "")
        ).fetchone()
        if request_row is None:
            raise HTTPException(status_code=404, detail="Заявка не найдена")

        timestamp = now_ms()
        try:
            db.execute(
                """
                INSERT INTO users(
                    login, name, pin_salt, pin_hash, is_owner, family_id,
                    avatar_url, phone, phone_normalized, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 0, ?, '', '', '', ?, ?)
                """,
                (
                    request_row["login"], request_row["name"],
                    request_row["pin_salt"], request_row["pin_hash"],
                    request_row["family_id"], timestamp, timestamp
                )
            )
        except sqlite3.IntegrityError:
            raise HTTPException(status_code=409, detail="Пользователь уже зарегистрирован")

        db.execute("DELETE FROM join_requests WHERE request_id = ?", (request_id,))
        db.commit()

    return {"status": "ok"}


@app.post("/family/join-requests/{request_id}/decline")
async def decline_join_request(request_id: int, payload: JoinRequestDecision):
    owner_name = str(payload.owner_name or "").strip()
    with db_connect() as db:
        owner = db.execute(
            "SELECT family_id, is_owner FROM users WHERE name = ? COLLATE NOCASE",
            (owner_name,)
        ).fetchone()
        if owner is None or not bool(owner["is_owner"]):
            raise HTTPException(status_code=403, detail="Только владелец может отклонять заявки")

        cursor = db.execute(
            "DELETE FROM join_requests WHERE request_id = ? AND family_id = ?",
            (request_id, owner["family_id"] or "")
        )
        db.commit()
        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="Заявка не найдена")

    return {"status": "ok"}


@app.post("/auth/register")
async def register_account(payload: RegisterRequest):
    login = normalize_login(payload.login)
    name = str(payload.name or "").strip()
    pin = validate_pin(payload.pin)
    phone = str(payload.phone or "").strip()
    phone_normalized = normalize_phone(phone)

    if len(login) < 2:
        raise HTTPException(status_code=400, detail="Слишком короткий логин")

    if len(name) < 2:
        raise HTTPException(status_code=400, detail="Слишком короткое имя")

    salt = secrets.token_hex(16)
    pin_hash = hash_pin(pin, salt)
    timestamp = now_ms()

    with db_connect() as db:
        total = db.execute("SELECT COUNT(*) FROM users").fetchone()[0]
        is_owner = bool(payload.is_owner or total == 0)
        family_id = str(payload.family_id or "").strip()
        if not family_id:
            family_id = secrets.token_hex(4).upper()

        try:
            cursor = db.execute(
                """
                INSERT INTO users(
                    login, name, pin_salt, pin_hash,
                    is_owner, family_id, avatar_url,
                    phone, phone_normalized,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, '', ?, ?, ?, ?)
                """,
                (
                    login,
                    name,
                    salt,
                    pin_hash,
                    1 if is_owner else 0,
                    family_id,
                    phone,
                    phone_normalized,
                    timestamp,
                    timestamp
                )
            )
            db.commit()
        except sqlite3.IntegrityError:
            raise HTTPException(
                status_code=409,
                detail="Такой логин или имя уже зарегистрированы"
            )

        user_id = int(cursor.lastrowid)

    return {
        "status": "ok",
        "user": {
            "user_id": user_id,
            "login": login,
            "name": name,
            "is_owner": is_owner,
            "family_id": family_id,
            "phone": phone,
            "phone_normalized": phone_normalized
        }
    }


@app.post("/auth/login")
async def login_account(payload: LoginRequest):
    login = normalize_login(payload.login)
    pin = validate_pin(payload.pin)

    with db_connect() as db:
        row = db.execute(
            """
            SELECT user_id, login, name, pin_salt, pin_hash,
                   is_owner, family_id, avatar_url,
                   phone, phone_normalized
            FROM users
            WHERE login = ? COLLATE NOCASE
            """,
            (login,)
        ).fetchone()

    if row is None:
        raise HTTPException(status_code=401, detail="Неверный логин или PIN")

    actual = hash_pin(pin, row["pin_salt"])
    if not hmac.compare_digest(actual, row["pin_hash"]):
        raise HTTPException(status_code=401, detail="Неверный логин или PIN")

    return {
        "status": "ok",
        "user": {
            "user_id": int(row["user_id"]),
            "login": row["login"],
            "name": row["name"],
            "is_owner": bool(row["is_owner"]),
            "family_id": row["family_id"] or "",
            "avatar_url": row["avatar_url"] or "",
            "phone": row["phone"] or "",
            "phone_normalized": row["phone_normalized"] or ""
        }
    }


@app.post("/profile/phone")
async def update_profile_phone(
    payload: ProfilePhoneRequest
):
    user_name = str(
        payload.user_name
        or ""
    ).strip()

    phone = str(
        payload.phone
        or ""
    ).strip()

    phone_normalized = normalize_phone(
        phone
    )

    if not user_name:
        raise HTTPException(
            status_code=400,
            detail="Не указано имя пользователя"
        )

    if phone and len(phone_normalized) < 10:
        raise HTTPException(
            status_code=400,
            detail="Некорректный номер телефона"
        )

    with db_connect() as db:
        row = db.execute(
            """
            SELECT user_id
            FROM users
            WHERE name = ? COLLATE NOCASE
            """,
            (user_name,)
        ).fetchone()

        if row is None:
            raise HTTPException(
                status_code=404,
                detail="Аккаунт не найден. Сначала зарегистрируйте пользователя."
            )

        if phone_normalized:
            duplicate = db.execute(
                """
                SELECT user_id, name
                FROM users
                WHERE phone_normalized = ?
                  AND user_id != ?
                """,
                (
                    phone_normalized,
                    int(row["user_id"])
                )
            ).fetchone()

            if duplicate is not None:
                raise HTTPException(
                    status_code=409,
                    detail="Этот номер уже привязан к другому пользователю"
                )

        db.execute(
            """
            UPDATE users
            SET phone = ?,
                phone_normalized = ?,
                updated_at = ?
            WHERE user_id = ?
            """,
            (
                phone,
                phone_normalized,
                now_ms(),
                int(row["user_id"])
            )
        )

        db.commit()

    packet = {
        "type": "users_sync",
        "users": all_server_users()
    }

    for client in list(
        connected_clients.values()
    ):
        try:
            await client.send_json(
                packet
            )
        except Exception:
            pass

    return {
        "status": "ok",
        "phone": phone,
        "phone_normalized": phone_normalized
    }


@app.get("/users")
async def users():
    return {
        "status": "ok",
        "users": all_server_users()
    }


@app.get("/chats/for-user/{user_name}")
async def get_chats_for_user(user_name: str):
    return {
        "status": "ok",
        "user_name": user_name,
        "chats": chats_for_user(user_name)
    }


@app.post("/chats/upsert")
async def upsert_chat(payload: ChatUpsertRequest):
    members = clean_members(payload.members, "")
    if not members:
        raise HTTPException(status_code=400, detail="У чата должны быть участники")

    chat_id = int(payload.chat_id or 0)
    if chat_id <= 0:
        with db_connect() as db:
            row = db.execute("SELECT COALESCE(MAX(chat_id), 0) + 1 FROM chats").fetchone()
            chat_id = int(row[0])

    save_chat_metadata(
        chat_id=chat_id,
        title=str(payload.title or "").strip(),
        chat_type=str(payload.chat_type or "GROUP").strip().upper(),
        members=members
    )

    return {
        "status": "ok",
        "chat": {
            "chat_id": chat_id,
            "title": str(payload.title or "").strip(),
            "chat_type": str(payload.chat_type or "GROUP").strip().upper(),
            "members": members,
            "updated_at": now_ms()
        }
    }


@app.get("/")
async def root():

    return {
        "app": "Очаг",
        "status": "ok",
        "version": APP_VERSION,
        "message": "Сервер Очаг работает"
    }


@app.get("/health")
async def health():

    with db_connect() as db:

        message_count = db.execute(
            """
            SELECT COUNT(*)
            FROM messages
            """
        ).fetchone()[0]

        pending_count = db.execute(
            """
            SELECT COUNT(*)
            FROM receipts
            WHERE delivered = 0
            """
        ).fetchone()[0]

    return {
        "status": "online",
        "version": APP_VERSION,
        "connected_users":
            list(
                connected_clients.keys()
            ),
        "stored_messages":
            message_count,
        "pending_deliveries":
            pending_count
    }


@app.get("/history/{user_name}")
async def history(
    user_name: str
):

    return {
        "status": "ok",
        "user_name": user_name,
        "messages":
            history_for_user(
                user_name
            )
    }


@app.post("/upload")
async def upload_file(
    file: UploadFile = File(...)
):

    original_name = file.filename or "file"

    extension = Path(
            original_name
        ).suffix

    saved_name = f"{uuid.uuid4().hex}{extension}"

    saved_path = UPLOAD_DIR / saved_name

    content = await file.read()

    with open(
        saved_path,
        "wb"
    ) as output_file:

        output_file.write(
            content
        )

    return {
        "status": "ok",
        "file_name": original_name,
        "stored_name": saved_name,
        "url":
            f"/uploads/{saved_name}"
    }



async def send_users_sync(websocket: WebSocket):
    await websocket.send_json({
        "type": "users_sync",
        "users": all_server_users()
    })


async def send_chat_sync(user_name: str, websocket: WebSocket):
    await websocket.send_json({
        "type": "chat_sync",
        "chats": chats_for_user(user_name)
    })


async def broadcast_presence():

    packet = {
        "type": "presence",
        "online_users":
            list(
                connected_clients.keys()
            )
    }

    dead = []

    for (
        user_name,
        websocket
    ) in list(
        connected_clients.items()
    ):

        try:
            await websocket.send_json(
                packet
            )

        except Exception:
            dead.append(
                user_name
            )

    for user_name in dead:
        connected_clients.pop(
            user_name,
            None
        )


async def send_to_other_users(
    sender_user: str,
    packet: dict,
    members=None
):

    delivered_online = 0
    delivered_names = []
    dead_connections = []

    for (
        target_name,
        target_socket
    ) in list(
        connected_clients.items()
    ):

        if (
            target_name ==
            sender_user
        ):
            continue

        if (
            members is not None
            and len(
                members
            ) > 0
            and target_name
                not in members
        ):
            continue

        try:

            await target_socket.send_json(
                packet
            )

            delivered_online += 1

            delivered_names.append(
                target_name
            )

            print(
                f"[DELIVER] "
                f"{sender_user} -> "
                f"{target_name}"
            )

        except Exception as error:

            print(
                f"[SEND ERROR] "
                f"{target_name}: "
                f"{error}"
            )

            dead_connections.append(
                target_name
            )

    for target_name in dead_connections:

        connected_clients.pop(
            target_name,
            None
        )

    return (
        delivered_online,
        delivered_names
    )


@app.websocket("/ws/{user_name}")
async def websocket_endpoint(
    websocket: WebSocket,
    user_name: str
):

    await websocket.accept()

    old_socket = connected_clients.get(
            user_name
        )

    if (
        old_socket is not None
        and old_socket is not websocket
    ):

        try:
            await old_socket.close(
                code=1000,
                reason="New connection opened"
            )
        except Exception:
            pass

    connected_clients[
        user_name
    ] = websocket

    print(
        f"[CONNECT] {user_name}"
    )

    print(
        "[ONLINE]",
        list(
            connected_clients.keys()
        )
    )

    await websocket.send_json(
        {
            "type":
                "connected",

            "user_name":
                user_name,

            "online_users":
                list(
                    connected_clients.keys()
                )
        }
    )

    await broadcast_presence()

    await send_users_sync(websocket)

    await send_chat_sync(
        user_name,
        websocket
    )

    await send_history_sync(
        user_name,
        websocket
    )

    try:

        while True:

            data = await websocket.receive_json()

            message_type = data.get(
                    "type"
                )

            if (
                message_type ==
                "typing"
            ):

                chat_id = int(
                    data.get(
                        "chat_id",
                        0
                    )
                    or 0
                )

                members = clean_members(
                    data.get(
                        "members",
                        []
                    ),
                    user_name
                )

                packet = {
                    "type": "typing",
                    "chat_id": chat_id,
                    "sender_name": user_name,
                    "is_typing": bool(
                        data.get(
                            "is_typing",
                            False
                        )
                    )
                }

                await send_to_other_users(
                    sender_user=user_name,
                    packet=packet,
                    members=members
                )

            elif (
                message_type ==
                "chat_message"
            ):

                message_id = data.get(
                        "message_id"
                    )

                chat_id = data.get(
                        "chat_id"
                    )

                sender_name = str(
                        data.get(
                            "sender_name",
                            user_name
                        )
                    )

                text = str(
                        data.get(
                            "text",
                            ""
                        )
                    ).strip()

                members = clean_members(
                        data.get(
                            "members",
                            []
                        ),
                        sender_name
                    )

                if not text:
                    continue

                packet = {
                    "type":
                        "chat_message",

                    "message_id":
                        message_id,

                    "chat_id":
                        chat_id,

                    "sender_name":
                        sender_name,

                    "text":
                        text,

                    "members":
                        members,

                    "chat_title":
                        str(
                            data.get(
                                "chat_title",
                                ""
                            )
                        ),

                    "chat_type":
                        str(
                            data.get(
                                "chat_type",
                                "PRIVATE"
                            )
                        ),

                    "created_at":
                        int(
                            time.time() *
                            1000
                        )
                }

                save_message(
                    {
                        **packet,
                        "media_type":
                            "TEXT"
                    }
                )

                print(
                    f"[CHAT] id={message_id} "
                    f"chat={chat_id} "
                    f"from={sender_name} "
                    f"text={text}"
                )

                (
                    delivered_online,
                    delivered_names
                ) = await send_to_other_users(
                    sender_user=user_name,
                    packet=packet,
                    members=members
                )

                for target_name in delivered_names:

                    mark_delivered(
                        int(
                            message_id
                        ),
                        target_name
                    )

                await websocket.send_json(
                    {
                        "type":
                            "server_ack",

                        "message_id":
                            message_id,

                        "chat_id":
                            chat_id,

                        "status":
                            "received",

                        "delivered_online":
                            delivered_online
                    }
                )

            elif (
                message_type ==
                "media_message"
            ):

                message_id = data.get(
                        "message_id"
                    )

                chat_id = data.get(
                        "chat_id"
                    )

                sender_name = str(
                        data.get(
                            "sender_name",
                            user_name
                        )
                    )

                media_type = str(
                        data.get(
                            "media_type",
                            "FILE"
                        )
                    )

                media_url = str(
                        data.get(
                            "media_url",
                            ""
                        )
                    )

                file_name = str(
                        data.get(
                            "file_name",
                            ""
                        )
                    )

                caption = str(
                        data.get(
                            "caption",
                            ""
                        )
                    )

                members = clean_members(
                        data.get(
                            "members",
                            []
                        ),
                        sender_name
                    )

                if not media_url:
                    continue

                packet = {
                    "type":
                        "media_message",

                    "message_id":
                        message_id,

                    "chat_id":
                        chat_id,

                    "sender_name":
                        sender_name,

                    "media_type":
                        media_type,

                    "media_url":
                        media_url,

                    "file_name":
                        file_name,

                    "caption":
                        caption,

                    "members":
                        members,

                    "chat_title":
                        str(
                            data.get(
                                "chat_title",
                                ""
                            )
                        ),

                    "chat_type":
                        str(
                            data.get(
                                "chat_type",
                                "PRIVATE"
                            )
                        ),

                    "created_at":
                        int(
                            time.time() *
                            1000
                        )
                }

                save_message(
                    packet
                )

                print(
                    f"[MEDIA] id={message_id} "
                    f"chat={chat_id} "
                    f"from={sender_name} "
                    f"type={media_type}"
                )

                (
                    delivered_online,
                    delivered_names
                ) = await send_to_other_users(
                    sender_user=user_name,
                    packet=packet,
                    members=members
                )

                for target_name in delivered_names:

                    mark_delivered(
                        int(
                            message_id
                        ),
                        target_name
                    )

                await websocket.send_json(
                    {
                        "type":
                            "server_ack",

                        "message_id":
                            message_id,

                        "chat_id":
                            chat_id,

                        "status":
                            "received",

                        "media":
                            True,

                        "delivered_online":
                            delivered_online
                    }
                )

            elif (
                message_type ==
                "read_receipt"
            ):

                chat_id = data.get(
                        "chat_id"
                    )

                message_ids = data.get(
                        "message_ids",
                        []
                    )

                members = data.get(
                        "members",
                        []
                    )

                if not isinstance(
                    message_ids,
                    list
                ):
                    message_ids = []

                if not isinstance(
                    members,
                    list
                ):
                    members = []

                clean_ids = []

                for item in message_ids:

                    try:
                        value = int(
                            item
                        )

                        if (
                            value > 0
                        ):
                            clean_ids.append(
                                value
                            )

                    except (
                        TypeError,
                        ValueError
                    ):
                        pass

                if not clean_ids:
                    continue

                mark_read(
                    clean_ids,
                    user_name
                )

                packet = {
                    "type":
                        "read_receipt",

                    "chat_id":
                        chat_id,

                    "reader_name":
                        user_name,

                    "message_ids":
                        clean_ids
                }

                print(
                    f"[READ] "
                    f"chat={chat_id} "
                    f"reader={user_name} "
                    f"ids={clean_ids}"
                )

                await send_to_other_users(
                    sender_user=user_name,
                    packet=packet,
                    members=members
                )


            elif (
                message_type in {
                    "call_invite",
                    "call_accept",
                    "call_reject",
                    "call_end",
                    "webrtc_offer",
                    "webrtc_answer",
                    "ice_candidate",
                    "call_mute",
                    "call_camera"
                }
            ):

                target_names = data.get(
                    "targets",
                    []
                )

                if not isinstance(
                    target_names,
                    list
                ):
                    target_names = []

                single_target = str(
                    data.get(
                        "target_name",
                        ""
                    )
                    or ""
                ).strip()

                if single_target:
                    target_names.append(
                        single_target
                    )

                clean_targets = []

                for target_name in target_names:
                    clean_target = str(
                        target_name
                        or ""
                    ).strip()

                    if (
                        clean_target
                        and clean_target != user_name
                        and clean_target not in clean_targets
                    ):
                        clean_targets.append(
                            clean_target
                        )

                packet = dict(data)
                packet["type"] = message_type
                packet["sender_name"] = user_name
                packet["sent_at"] = now_ms()
                packet.pop("targets", None)
                packet.pop("target_name", None)

                delivered_to = []

                for target_name in clean_targets:
                    target_socket = connected_clients.get(
                        target_name
                    )

                    if target_socket is None:
                        continue

                    try:
                        await target_socket.send_json(
                            packet
                        )
                        delivered_to.append(
                            target_name
                        )
                    except Exception:
                        pass

                await websocket.send_json(
                    {
                        "type": "call_signal_ack",
                        "signal_type": message_type,
                        "call_id": str(
                            data.get(
                                "call_id",
                                ""
                            )
                        ),
                        "delivered_to": delivered_to
                    }
                )

                print(
                    f"[CALL] type={message_type} "
                    f"from={user_name} "
                    f"to={delivered_to}"
                )

            elif (
                message_type ==
                "users_request"
            ):

                await send_users_sync(
                    websocket
                )

            elif (
                message_type ==
                "chat_upsert"
            ):

                chat_id = int(
                    data.get("chat_id")
                    or 0
                )

                title = str(
                    data.get("chat_title", "")
                ).strip()

                chat_type = str(
                    data.get("chat_type", "GROUP")
                ).strip().upper()

                members = clean_members(
                    data.get("members", []),
                    user_name
                )

                old_members = []

                if chat_id > 0:
                    with db_connect() as db:
                        old_row = db.execute(
                            """
                            SELECT members_json
                            FROM chats
                            WHERE chat_id = ?
                            """,
                            (chat_id,)
                        ).fetchone()

                    if old_row is not None:
                        try:
                            old_members = json.loads(
                                old_row["members_json"]
                                or "[]"
                            )
                        except Exception:
                            old_members = []

                    if not isinstance(
                        old_members,
                        list
                    ):
                        old_members = []

                if chat_id <= 0:
                    with db_connect() as db:
                        row = db.execute(
                            "SELECT COALESCE(MAX(chat_id), 0) + 1 FROM chats"
                        ).fetchone()
                        chat_id = int(row[0])

                save_chat_metadata(
                    chat_id=chat_id,
                    title=title,
                    chat_type=chat_type,
                    members=members
                )

                packet = {
                    "type": "chat_updated",
                    "chat_id": chat_id,
                    "chat_title": title,
                    "chat_type": chat_type,
                    "members": members,
                    "updated_at": now_ms()
                }

                notify_members = []

                for member in (
                    old_members +
                    members
                ):
                    clean_member = str(
                        member
                        or ""
                    ).strip()

                    if (
                        clean_member
                        and clean_member
                        not in notify_members
                    ):
                        notify_members.append(
                            clean_member
                        )

                for member in notify_members:
                    target = connected_clients.get(
                        member
                    )

                    if target is None:
                        continue

                    try:
                        await target.send_json(
                            packet
                        )
                    except Exception:
                        pass

            elif (
                message_type ==
                "history_request"
            ):

                await send_history_sync(
                    user_name,
                    websocket
                )

            else:

                print(
                    f"[UNKNOWN] "
                    f"user={user_name} "
                    f"type={message_type}"
                )

    except WebSocketDisconnect:
        pass

    except Exception as error:

        print(
            f"[ERROR] "
            f"{user_name}: "
            f"{error}"
        )

    finally:

        if (
            connected_clients.get(
                user_name
            ) is websocket
        ):

            connected_clients.pop(
                user_name,
                None
            )

        print(
            f"[DISCONNECT] "
            f"{user_name}"
        )

        print(
            "[ONLINE]",
            list(
                connected_clients.keys()
            )
        )

        await broadcast_presence()
