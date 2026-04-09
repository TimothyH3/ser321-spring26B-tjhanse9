# StringConcatenation Debugging Exercise

## Overview
The stringconcatenation service is implemented in both client and server, but has **4 bugs** that prevent it from working correctly according to the protocol specification.

The Correct Protocol is in the README.md

---

## The 4 Bugs

### Bug #1:  <string1 request>
**Location:** `SockClient.java`, line 77

**The Problem:**
According to protocol, the StringConcatenation request from the client side should give "type", "string1", and "string2"; but the client feeds the server "type", "str1", and "string2", so the server gives an error, "Field string1 does not exist in request".

**The Fix:**
Replace "str1" with "string1" on the SockClient script.

**Why it matters:** 
Without this bugfix, the client never sends a properly formatted request and the server cannot continue to process it.

**How did you find this:**
Located by reading the code.

### Bug #2:  <result request>
**Location:** `SockServer.java`, line 188

**The Problem:**
According to protocol, the StringConcatenation response from the server side should be formatted {"result":"(result)","ok":true,"type":"concat"}; but the server actually responds {"combined":(result),"ok":true,"type":"concat"}, so the client gives an error, "Success, but no known result key found.".

**The Fix:**
Replace "combined" with "result" on the SockServer script.

**Why it matters:** 
Without this bugfix, the server never sends a properly formatted response and the client cannot properly recieve it.

**How did you find this:**
Located by testing after first bugfix and reading the code.

### Bug #3:  <string2 testfield>
**Location:** `SockServer.java`, line 183

**The Problem:**
According to protocol, the server should handle missing fields with error response {"ok":false, "message": (...)}, so- the server attempts to use string2 without running testField on it first. If a client forgets string2 or sends the wrong data type, the server throws an unhandled Java Exception.

**The Fix:**
Add the validation block for string2:
res = testField(req, "string2"); if (!res.getBoolean("ok")) { return res; }

**Why it matters:** 
Without this bugfix, invalid client requests will crash the server instead of throwing an error.

**How did you find this:**
Located by testing after first bugfix and reading the code.

### Bug #4:  <response formatting>
**Location:** `SockServer.java`, line 191

**The Problem:**
According to protocol, "type" field in the response should be "stringconcatenation", but SockServer sends "concat" instead.

**The Fix:**
Replace "concat" with "stringconcatenation" under res.put("type", ...)

**Why it matters:** 
This is simply a violation of protocol. The current client actually accepts the response by just checking for the result key, but the server is technically sending invalid messages.

**How did you find this:**
Studying protocol and comparing it to both scripts.
