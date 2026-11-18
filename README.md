# http4s-circe-example

A small, self-contained [http4s](https://http4s.org) server showing a wide range of common techniques:
routing, JSON with circe, streaming, gzip, static files, form handling, state with `Ref`,
calling other services with an HTTP client, HTML with scalatags, and testing routes with http4s-munit.

## Stack

| Library | Version |
| --- | --- |
| Scala | 2.13.18 |
| sbt | 1.13.1 |
| http4s (Ember server & client, DSL, circe) | 0.23.38 |
| cats-effect | 3.7.1 |
| circe (generic, literal) | 0.14.17 |
| log4cats-slf4j + logback | 2.8.0 / 1.6.6 |
| http4s-scalatags | 0.25.4 |
| http4s-munit (tests) | 3.0.0 |

## Prerequisites

- JDK 17 or newer (tested on JDK 25)
- [sbt](https://www.scala-sbt.org/download/)

## Running

```bash
sbt run            # start the server on http://localhost:8080
sbt ~reStart       # or: restart automatically on code changes (sbt-revolver)
sbt test           # run the route tests
```

## Endpoints

All examples assume the server is running on `http://localhost:8080`.

### Basics

| Method | Path | Returns |
| --- | --- | --- |
| GET | [/greet](http://localhost:8080/greet) | JSON from a case class: `{"message":"hello there"}` |
| GET | [/hello/{name}](http://localhost:8080/hello/Dino) | Text greeting plus your remote address |
| GET | [/hello?name=](http://localhost:8080/hello?name=Dino) | Text greeting, using a query parameter matcher |
| GET | [/literal](http://localhost:8080/literal) | JSON from a circe `json"..."` literal |
| GET | [/random](http://localhost:8080/random) | A random digit (0-9), using `cats.effect.std.Random` |

### Streaming, compression and static files

| Method | Path | Returns |
| --- | --- | --- |
| GET | [/gzip](http://localhost:8080/gzip) | 3500 chars of text, gzipped by the `GZip` middleware |
| GET | [/mystream](http://localhost:8080/mystream) | An endless chunked stream, emitting elapsed time every 2 seconds |
| GET | [/fs/hello.txt](http://localhost:8080/fs/hello.txt) | Static text served from the classpath |
| GET | [/fs/form.html](http://localhost:8080/fs/form.html) | An HTML form that posts to `/handle_form` |
| GET | [/slow](http://localhost:8080/slow) | Sleeps 4 seconds, then replies |

```bash
curl --compressed http://localhost:8080/gzip
curl -N http://localhost:8080/mystream      # Ctrl+C to stop
```

### POST

| Method | Path | Returns |
| --- | --- | --- |
| POST | /echo | Whatever body you sent |
| POST | /handle_form | `Hello {username}`, decoded via `UrlForm` |

```bash
curl -d 'woof' http://localhost:8080/echo
curl -d 'username=Dino' http://localhost:8080/handle_form
```

### State

| Method | Path | Returns |
| --- | --- | --- |
| GET | [/counter](http://localhost:8080/counter) | Incrementing number using a global `var`. **Deliberately hacky**: not safe under concurrent requests |
| GET | [/counter2](http://localhost:8080/counter2) | Incrementing number using `Ref[IO, Int]`, the correct, atomic way |

### HTTP client examples

A single Ember client is created at startup and shared by all routes.

Calling our own endpoints:

| Path | Shows |
| --- | --- |
| [/client/slow](http://localhost:8080/client/slow) | Calls `/slow` with a 1s `timeoutTo`, falling back to a canned value |
| [/client/twiceslow](http://localhost:8080/client/twiceslow) | Calls `/slow` twice **in parallel** with `parTupled` (about 4s total, not 8s) |

Talking to the external service <https://jsonplaceholder.typicode.com/>:

| Path | Shows |
| --- | --- |
| [/client/todos](http://localhost:8080/client/todos), [/client/todos/1](http://localhost:8080/client/todos/1) | Upstream JSON passed through as a string |
| [/client/todos/999](http://localhost:8080/client/todos/999) | Upstream 404 mapped to our own 404 |
| [/client/users](http://localhost:8080/client/users), [/client/users/1](http://localhost:8080/client/users/1) | Upstream JSON passed through as a string |
| [/client/users/999](http://localhost:8080/client/users/999) | Errors turned into a 200 with an error message |
| [/client/posts](http://localhost:8080/client/posts), [/client/posts/1](http://localhost:8080/client/posts/1) | JSON decoded into `Post` case classes, then re-encoded |
| [/client/posts/999](http://localhost:8080/client/posts/999) | Any error replaced with a dummy `Post` |

### OpenAI image generation

Generates an image with OpenAI's `gpt-image-1-mini` model and returns an HTML page (built with scalatags) with the image inline.
(The old DALL-E models were retired by OpenAI in May 2026.)

You need an OpenAI API key with credit in the `OPENAI_API_KEY` environment variable **before** starting sbt. Each call is charged to your OpenAI account.

```bash
export OPENAI_API_KEY=sk-...          # bash / zsh
$env:OPENAI_API_KEY = "sk-..."        # PowerShell
set OPENAI_API_KEY=sk-...             # cmd
```

Then open:

- <http://localhost:8080/openai/image?prompt=a+ginger+cat,+wearing+round+black+glasses,+riding+a+bike+and+smoking+a+pipe,+photorealistic>

If the key is missing you get a `400`. If OpenAI rejects the request (bad key, no credit, moderation), the page shows OpenAI's error message.

## Project layout

```text
build.sbt                          dependencies
project/plugins.sbt                sbt-revolver
src/main/scala/MyMain.scala        all routes, client examples and server wiring
src/main/resources/                logback.xml, hello.txt, form.html (served under /fs)
src/test/scala/MainRoutesSuite.scala  route tests with http4s-munit
```
