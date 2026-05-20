# gRPC API Documentation

This document describes the gRPC API provided by the market data backend server (MarketData service).

## Service: MarketData

The `MarketData` service provides real-time market data streaming, historical quotes, and order book information.

**Address:** `localhost:9090` (default)

---

## RPC Methods

### 1. ListInstruments

**Request:** `ListInstrumentsRequest` (empty)

**Response:** `ListInstrumentsResponse`

**Description:** Retrieve all available trading instruments.

**Response Fields:**
- `instruments: repeated Instrument` — list of available instruments

**Example:**
```python
request = pb2.ListInstrumentsRequest()
response = stub.ListInstruments(request, timeout=5.0)
for inst in response.instruments:
    print(f"id={inst.id}, ticker={inst.ticker}, name={inst.name}")
```

---

### 2. GetInstrument

**Request:** `GetInstrumentRequest`

**Response:** `Instrument`

**Description:** Get metadata for a specific instrument.

**Request Fields:**
- `instrument_id: uint32` — ID of the instrument

**Response Fields:**
- `id: uint32` — instrument ID
- `ticker: string` — ticker symbol
- `name: string` — instrument name
- `currency: string` — currency code
- `lot_size: uint32` — minimum lot size
- `price_step_micro: int64` — minimum price step in micro-units
- `is_active: bool` — whether the instrument is actively trading

**Example:**
```python
request = pb2.GetInstrumentRequest(instrument_id=1)
instrument = stub.GetInstrument(request, timeout=5.0)
print(f"Ticker: {instrument.ticker}")
print(f"Name: {instrument.name}")
print(f"Price step: {instrument.price_step_micro}")
```

---

### 3. GetQuote

**Request:** `GetQuoteRequest`

**Response:** `Quote`

**Description:** Get the current quote (last update) for an instrument.

**Request Fields:**
- `instrument_id: uint32` — ID of the instrument

**Response Fields:**
- `instrument_id: uint32` — instrument ID
- `ticker: string` — ticker symbol
- `price_micro: int64` — current price in micro-units (price × 1,000,000)
- `bid_micro: int64` — bid price in micro-units
- `ask_micro: int64` — ask price in micro-units
- `open_micro: int64` — day open price in micro-units
- `high_micro: int64` — day high price in micro-units
- `low_micro: int64` — day low price in micro-units
- `day_volume: uint64` — total volume for the trading day
- `change_bps: int32` — price change relative to open in basis points (bps)
- `timestamp_ns: int64` — timestamp of last update (Unix nanoseconds UTC)

**Example:**
```python
request = pb2.GetQuoteRequest(instrument_id=1)
quote = stub.GetQuote(request, timeout=5.0)
print(f"Price: {quote.price_micro / 1e6}")
print(f"Bid: {quote.bid_micro / 1e6}, Ask: {quote.ask_micro / 1e6}")
```

---

### 4. GetCandles

**Request:** `GetCandlesRequest`

**Response:** `GetCandlesResponse`

**Description:** Retrieve OHLCV candles for a specific instrument and time range.

**Request Fields:**
- `instrument_id: uint32` — ID of the instrument
- `interval: string` — candle interval: `"1m"`, `"5m"`, `"15m"`, `"1h"`, `"1d"`
- `from_ns: int64` — start time (Unix nanoseconds UTC); 0 = no lower bound
- `to_ns: int64` — end time (Unix nanoseconds UTC); 0 = current time
- `limit: uint32` — max candles to return (0 = 100, max = 1000)
- `include_open: bool` — include current incomplete candle

**Response Fields:**
- `candles: repeated Candle` — list of candles

**Candle Fields:**
- `instrument_id: uint32`
- `interval: string`
- `open_time_ns: int64` — candle open time (Unix nanoseconds UTC)
- `open_micro: int64`, `high_micro: int64`, `low_micro: int64`, `close_micro: int64` — OHLC in micro-units
- `volume: uint64` — total trade volume
- `trades: uint32` — number of trades
- `is_closed: bool` — whether the candle is finalized

**Example:**
```python
request = pb2.GetCandlesRequest(
    instrument_id=1,
    interval="1m",
    limit=10,
    include_open=True
)
response = stub.GetCandles(request, timeout=5.0)
for candle in response.candles:
    print(f"Open: {candle.open_micro}, Close: {candle.close_micro}, Volume: {candle.volume}")
```

---

### 5. GetOrderBook

**Request:** `GetOrderBookRequest`

**Response:** `OrderBook`

**Description:** Get the current order book (bid/ask levels) for an instrument.

**Request Fields:**
- `instrument_id: uint32` — ID of the instrument

**Response Fields:**
- `instrument_id: uint32`
- `bids: repeated OrderBookLevel` — 10 bid levels (descending price)
- `asks: repeated OrderBookLevel` — 10 ask levels (ascending price)
- `timestamp_ns: int64` — order book snapshot time

**OrderBookLevel Fields:**
- `price_micro: int64` — price in micro-units
- `quantity: uint64` — quantity at this level

**Example:**
```python
request = pb2.GetOrderBookRequest(instrument_id=1)
ob = stub.GetOrderBook(request, timeout=5.0)
for bid in ob.bids:
    print(f"Bid: {bid.price_micro / 1e6} x {bid.quantity}")
```

---

### 6. StreamTicks

**Request:** `StreamTicksRequest` (streaming)

**Response:** `Tick` (stream)

**Description:** Stream live tick data for one or more instruments.

**Request Fields:**
- `instrument_ids: repeated uint32` — filter by instrument IDs; empty = all instruments

**Response Fields (repeated):**
- `instrument_id: uint32`
- `ticker: string`
- `timestamp_ns: int64` — tick time (Unix nanoseconds UTC)
- `price_micro: int64` — trade price in micro-units
- `volume: uint32` — trade volume
- `bid_micro: int64` — bid price in micro-units
- `ask_micro: int64` — ask price in micro-units

**Example:**
```python
request = pb2.StreamTicksRequest(instrument_ids=[1, 2, 3])
for tick in stub.StreamTicks(request, timeout=60.0):
    print(f"{tick.ticker}: {tick.price_micro / 1e6} @ {tick.timestamp_ns}")
```

---

### 7. GetQuotesRange

**Request:** `GetQuotesRangeRequest` (streaming)

**Response:** `Tick` (stream)

**Description:** Stream ticks for a time range and set of instruments. Filters ticks by timestamp and instrument ID.

**Request Fields:**
- `instrument_ids: repeated uint32` — filter by instrument IDs; empty = all instruments
- `from_ns: int64` — start time (Unix nanoseconds UTC); 0 = stream from now
- `to_ns: int64` — end time (Unix nanoseconds UTC); 0 = stream indefinitely (until client cancels)

**Response Fields (repeated):**
- Same as `Tick` from `StreamTicks`

**Semantics:**
- **If `from_ns == 0`:** Start streaming from the current time (`time.Now()`).
- **If `to_ns == 0`:** Never stop; stream until the client closes the RPC.
- **If `to_ns > 0` and `tick.TimestampNs > to_ns`:** Stop streaming and close the connection.

**Example — Last 5 minutes of live data:**
```python
import time

now_ns = int(time.time() * 1e9)
five_min_ns = 5 * 60 * int(1e9)

request = pb2.GetQuotesRangeRequest(
    instrument_ids=[1, 2],
    from_ns=now_ns - five_min_ns,
    to_ns=0,  # stream indefinitely
)

quotes_by_instrument = {}
for tick in stub.GetQuotesRange(request, timeout=30.0):
    if tick.instrument_id not in quotes_by_instrument:
        quotes_by_instrument[tick.instrument_id] = []
    quotes_by_instrument[tick.instrument_id].append(tick)
```

**Example — Historical range query:**
```python
start_ns = 1234567890000000000  # some past Unix ns timestamp
end_ns = 1234567900000000000    # later timestamp

request = pb2.GetQuotesRangeRequest(
    instrument_ids=[1],
    from_ns=start_ns,
    to_ns=end_ns,
)

for tick in stub.GetQuotesRange(request, timeout=5.0):
    print(f"{tick.timestamp_ns}: {tick.price_micro / 1e6}")
```

---

## Common Message Types

### Instrument

```protobuf
message Instrument {
    uint32 id = 1;
    string ticker = 2;
    string name = 3;
    string currency = 4;
    uint32 lot_size = 5;
    int64 price_step_micro = 6;
    bool is_active = 7;
}
```

### Tick

```protobuf
message Tick {
    uint32 instrument_id = 1;
    string ticker = 2;
    int64 timestamp_ns = 3;
    int64 price_micro = 4;
    uint32 volume = 5;
    int64 bid_micro = 6;
    int64 ask_micro = 7;
}
```

---

## Error Handling

All RPCs may return gRPC errors with standard status codes:
- `UNAVAILABLE` — server is unreachable or offline
- `INVALID_ARGUMENT` — invalid request parameters (e.g., unknown interval)
- `NOT_FOUND` — instrument or data not found
- `INTERNAL` — server-side error
- `DEADLINE_EXCEEDED` — request timeout

**Example:**
```python
import grpc

try:
    response = stub.GetQuote(request, timeout=5.0)
except grpc.RpcError as e:
    print(f"Error {e.code()}: {e.details()}")
    if e.code() == grpc.StatusCode.NOT_FOUND:
        print("Instrument not found")
```

---

## Connection

**Default Address:** `localhost:9090`

**Channel Creation (Python):**
```python
import grpc

channel = grpc.insecure_channel("localhost:9090")
stub = marketdata_pb2_grpc.MarketDataStub(channel)
```

**Note:** For production use, configure TLS credentials instead of `insecure_channel`.

---

## Units and Conventions

- **Prices:** All prices are in micro-units (actual price × 1,000,000).
  - Example: price_micro = 100,000,000 means 100.0 RUB
- **Timestamps:** Unix nanoseconds UTC (seconds since epoch × 1,000,000,000).
- **Volumes:** Raw trade/candle volume (lot count).
- **Basis points (bps):** Used for percentage changes (1 bps = 0.01%).

---

## Testing

Use the provided Python helper scripts:

- **`check_grpc.py`** — Basic smoke test (ListInstruments, GetQuote)
- **`check_grpc_array.py`** — Stream and print quote arrays per instrument
- **`tests/test_check_grpc.py`** — Unit tests for proto compilation

**Run tests:**
```bash
python3 -m unittest discover tests
```

**Run array helper:**
```bash
python3 check_grpc_array.py --addr localhost:9090 --timeout 20.0
```
