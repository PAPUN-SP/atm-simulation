# ATM Simulation System

A Java ATM simulator with a **REST backend**, an **ATM-style web frontend**, and the original **console app**.
Built with core Java (OOP + Collections), with no external libraries or build tools needed.

## Features
- User authentication with PIN (card blocks after 3 wrong attempts)
- Withdraw / Deposit money (with limits and validation)
- Balance enquiry
- Mini statement (last 5 transactions)
- PIN change
- Session tokens with automatic 2-minute idle logout in the web UI

## Project structure
```
atm-simulation/
├── Dockerfile
├── docker-compose.yml
├── README.md
├── src/main/java/com/atm/
│   ├── Account.java        # entity: balance, PIN, history (ArrayList)
│   ├── Transaction.java    # immutable record
│   ├── Bank.java           # business rules, accounts & sessions (HashMap)
│   ├── AtmException.java   # business error with status code
│   ├── AtmServer.java      # HTTP server: REST API + serves frontend
│   └── ConsoleApp.java     # console version
└── web/index.html          # frontend (single page)
```

## Run with Docker (recommended)
```bash
docker compose up --build
# or
docker build -t atm-simulation .
docker run -p 8080:8080 atm-simulation
```
Open **http://localhost:8080**.

Console version inside Docker:
```bash
docker run -it --rm atm-simulation java -cp out com.atm.ConsoleApp
```

## Run without Docker (JDK 17+)
```bash
mkdir out && javac -d out $(find src -name "*.java")
java -cp out com.atm.AtmServer        # web app on :8080
java -cp out com.atm.ConsoleApp       # console app
```
Environment variables: `PORT` (default 8080), `WEB_DIR` (default `web`).

## Demo accounts
| Account | PIN  | Holder       | Opening balance |
|---------|------|--------------|-----------------|
| 1001    | 1234 | Alice Kumar  | 5,000           |
| 1002    | 4321 | Bob Rao      | 12,000          |
| 1003    | 1111 | Carol Singh  | 250             |

## Rules
- Withdrawals: multiples of 100, max 20,000 per transaction, cannot exceed balance
- Deposits: max 50,000 per transaction
- New PIN: exactly 4 digits, different from the old one

## REST API
Authenticated endpoints need `Authorization: Bearer <token>`.

| Method | Endpoint         | Body                         | Description        |
|--------|------------------|------------------------------|--------------------|
| POST   | `/api/login`     | `{"account","pin"}`          | Returns `token`    |
| POST   | `/api/logout`    |                              | End session        |
| GET    | `/api/balance`   |                              | Holder & balance   |
| POST   | `/api/withdraw`  | `{"amount"}`                 | Withdraw cash      |
| POST   | `/api/deposit`   | `{"amount"}`                 | Deposit cash       |
| GET    | `/api/statement` |                              | Last 5 transactions|
| POST   | `/api/pin`       | `{"oldPin","newPin"}`        | Change PIN         |

Example:
```bash
TOKEN=$(curl -s -X POST localhost:8080/api/login -d '{"account":"1001","pin":"1234"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/balance
curl -X POST -H "Authorization: Bearer $TOKEN" -d '{"amount":500}' localhost:8080/api/withdraw
```

## Notes
Data is held in memory, so it resets when the server restarts. To persist it, replace the maps in `Bank` with a database (JDBC) layer.
