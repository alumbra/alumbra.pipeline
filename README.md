# alumbra.ring

This library provides a Ring handler implementation for [GraphQL][graphql] query
execution, as well as the interactive [GraphiQL][graphiql] environment. It uses
pluggable components that consume/produce data structures as described in
[alumbra.spec][alumbra-spec].

[graphql]: http://graphql.org
[graphiql](https://github.com/graphql/graphiql)
[alumbra-spec]: https://github.com/alumbra/alumbra.spec

## Usage

### GraphQL Endpoint

```clojure
(require '[alumbra.ring :as ql])

(def app
  (ql/handler
    {:schema   (clojure.java.io/resource "Schema.gql")
     :context  (fn [request] (read-auth request))
     :executor (fn [context canonical-operation] ...)}))

(defonce server
  (start-server #'app {:port 8080}))
```

### GraphiQL Web UI

You can create a Ring handler exposing a [GraphiQL][graphiql] web UI for
interactive GraphQL query execution using `alumbra.ring.graphiql/handler`:

```clojure
(require '[alumbra.ring.graphiql :as graphiql])

(def graphiql
  (graphiql/handler "/path/to/graphql"))

(defonce server
  (start-server #'graphiql {:port 8080}))
```

Assets will – by default – be loaded from [cdnjs][cdnjs]. See the docstring
on how to customize this behaviour.

[cdnjs]: https://cdnjs.com/

## License

```
MIT License

Copyright (c) 2016 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
