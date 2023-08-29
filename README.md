# Payment Driver

This project was made with Spring Boot v2.7.14.

## Setup

1. Add envionment variables with the following names:
   - `DEVOURIN_TERMINAL` - An ID for your computer (linked to the payment devices)
   - `DEVOURIN_SERVER_URL` - The base URL for your server. The project will use this to get a list of all the payment devices connected to your computer. The API for that is a GET request that hits `${DEVOURIN_SERVER_URL}/payment/getDeviceList?terminalId=${DEVOURIN_TERMINAL}`.
2. Import the project into Spring Tools Suite.

### Payment Device Format
 Please make sure the devices are in the following format:
```json
{
    "protocol": "",
    "address": "",
    "model": ""
}
```
As of August 2023, the only valid value for `protocol` is `"RS232"` and the only valid value for `model` is `"NETS"`
