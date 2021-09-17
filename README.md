# http4s-circe-example

Example of http4s server with various endpoints

## Endpoints

/greet
=> json

/hello/{name}
=> text

/hello?name=
=> text

/literal
=> json

/gzip
=> lots of gzipped text

/mystream
=> an endless chunked stream

/fs/hello.txt
=> static text from classpath

/twirl
=> HTML from a Play Twirl template

POST with some payload, you should get it back(use postman or similar)
/echo
=> Whatever you sent


