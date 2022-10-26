# http4s-circe-example

Example of http4s server with various endpoints

## Endpoints

http://localhost:8080/greet
=> json

http://localhost:8080/hello/{name}
=> text

http://localhost:8080/hello?name=
=> text

http://localhost:8080/literal
=> json

http://localhost:8080/gzip
=> lots of gzipped text

http://localhost:8080/mystream
=> an endless chunked stream

http://localhost:8080/fs/hello.txt
=> static text from classpath

http://localhost:8080/twirl
=> HTML from a Play Twirl template

POST with some payload, you should get it back(use postman or similar)

http://localhost:8080/echo
=> Whatever you sent

http://localhost:8080/random
=> a random digit

http://localhost:8080/counter
=> incrementing digit (hacky)

http://localhost:8080/counter2
=> incrementing digit (Better, with Ref)

(Client examples talking to external web service https://jsonplaceholder.typicode.com/)

- http://localhost:8080/client/users
- http://localhost:8080/client/users/1
- http://localhost:8080/client/users/999
- http://localhost:8080/client/posts
- http://localhost:8080/client/posts/1
- http://localhost:8080/client/posts/999
- http://localhost:8080/client/todos
- http://localhost:8080/client/todos/1
- http://localhost:8080/client/todos/999
