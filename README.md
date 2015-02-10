# Exchange Rates

## Running this app

**Prerequisites:**
- Play framework: 1.2.5
- Cassandra 1.2.13

The app is not ready for production and only local testing was done while developing. **Tested only in chrome**.

**Steps to run:**
- Clone this repo
- Get into cloned app directory
- Make sure Cassandra is running on `127.0.0.1:9160` then create necessary Keyspace for this app: `path/to/cassandra-cli -host localhost --file cassandra/schema-for-cli.txt`
- Execute in terminal `play deps`
- Execute in terminal `play run`
- Open page http://localhost:9000 in browser.

### Production
Application is not ready for any sort of deployment as variables are not in config and config is not environment specific and it is not known what is that deployment environment.

### Bugs
There should be bugs as in all software, but one thing is obvious that after testing locally with Apache Bench, in the middle of development, application crashed after ~2000 requests after 10 mins, leaving bunch of threads running and out of memory. I do suspect Futures implementation.

### Bottleneck
As I mentioned above I suspect there is a bug in Futures implementation of response data and this is the bottleneck now.
