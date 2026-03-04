### REST vs gRPC

|Factor|REST (HTTP/JSON)|gRPC (HTTP/2 +Protobuf)|
|---|---|---|
|Payload size|~120 bytes JSON per ping|~30 bytes Protobuf (4x smaller)|
|Throughput|~50k req/s per Go instance|~200k req/s per Go instance|
|Browser support|Native (fetch API)|Requires grpc-web proxy|
|Debugging|curl, Postman, browser devtools|Requires grpcurl or Postman gRPC|
|Schema enforcement|Optional (OpenAPI)|Mandatory (.proto files)|
|Go ecosystem support|Excellent|Excellent (google.golang.org/grpc)|
|Learning curve|None|Moderate (proto IDL, codegen)|


### Decision v1: REST

REST was chosen for v1 of this project for three reasons: 
 - the load test simulator (Locust) natively speaksHTTP, 
 - debugging GPS ingestion issues is simpler with curl, 
 - the payload size difference is negligible at the current scale of 2,000 driver at the same time.

However, the Go service is structured so that the transport layer (handler) is decoupled from the business
logic (validation + channel push). Migrating to gRPC would only require replacing the handler.

gRPC can be the correct choice when: 
 - payload volume exceeds 100k pings/second
 - bidirectionalstreaming is needed (server push to trucks
 - multiple downstream consumers need a strict schema contract via .proto files.
 
### Proto definition for v2:

```
// proto/telemetry/v1/telemetry.proto
syntax = "proto3";
package telemetry.v1;
message GPSEvent {
string driver_uid = 1;
double lat = 2;
double lng = 3;
google.protobuf.Timestamp timestamp = 4;
}
service TelemetryService {
rpc IngestEvent(GPSEvent) returns (IngestResponse);
// Future: bidirectional streaming
// rpc StreamEvents(stream GPSEvent) returns (stream IngestResponse);
```