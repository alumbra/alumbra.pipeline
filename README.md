# alumbra.ring

This library provides a Ring handler implementation for [GraphQL][graphql] query
execution, as well as the interactive [GraphiQL][graphiql] environment. It uses
pluggable components that consume/produce data structures as described in
[alumbra.spec][alumbra-spec].

[![Build Status](https://travis-ci.org/alumbra/alumbra.ring.svg?branch=master)](https://travis-ci.org/alumbra/alumbra.ring)
[![Clojars Project](https://img.shields.io/clojars/v/alumbra/ring.svg)](https://clojars.org/alumbra/ring)

[graphql]: http://graphql.org
[graphiql]: https://github.com/graphql/graphiql
[alumbra-spec]: https://github.com/alumbra/alumbra.spec

## Usage

### GraphQL Execution

To create a GraphQL-capable endpoint, you need to supply a series of
[alumbra.spec][alumbra-spec]-compatible components:

```clojure
(require '[alumbra.ring.graphql :as graphql])

(graphql/handler
  {:parser        ...
   :validator     ...
   :canonicalizer ...
   :context       (fn [request] (read-auth request))
   :executor      (fn [context canonical-operation] ...)})
```

Note that:

- `:parser` should consume an `InputStream` and return a value conforming to
  either `:alumbra/document` or `:alumbra/parser-errors`,
- `:validator` should consume an `:alumbra/document` and return either `nil` or
  a value conforming to `:alumbra/validation-errors`,
- `:canonicalizer` should consume a validated `:alumbra/document` and return a
  value conforming to `:alumbra/canonical-document`,
- `:context` should consume a Ring request map and produce any value
  representing the context of the GraphQL query,
- `:executor` should consume the value produced by `:context`, as well as an
  `:alumbra/canonical-operation`, and produce the resolved value as a map.

A variant of this handler that uses pre-defined components can be found in
the main [alumbra][alumbra] repository.

[alumbra]: https://github.com/alumbra/alumbra

### GraphiQL Web UI

To create a Ring handler exposing the interactive [GraphiQL][graphiql]
environment you just have to supply the path (or URL) of your GraphQL endpoint.

```clojure
(require '[alumbra.ring.graphiql :as graphiql])
(graphiql/handler "/path/to/graphql")
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
