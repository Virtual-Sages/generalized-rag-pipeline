from enum import IntEnum

class AllowedHttpResponseCodes(IntEnum):
    OK = 200
    UNAUTHORIZED = 401
    PAYLOAD_TOO_LARGE = 413        # context too long
    UNPROCESSABLE_ENTITY = 422     # correct request but the request had bad/unsupported values
    TOO_MANY_REQUESTS = 429
    INTERNAL_SERVER_ERROR = 500
    BAD_GATEWAY = 502              # provider unreachable
    GATEWAY_TIMEOUT = 504
