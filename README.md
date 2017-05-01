# alumbra.pipeline

This library provides a pipeline implementation for [GraphQL][graphql] query
execution, as well as a Ring-compatible handler encapsulating this task. It uses
pluggable components that consume/produce data structures as described in
[alumbra.spec][alumbra-spec].

[![Build Status](https://travis-ci.org/alumbra/alumbra.pipeline.svg?branch=master)](https://travis-ci.org/alumbra/alumbra.pipeline)
[![Clojars Project](https://img.shields.io/clojars/v/alumbra/pipeline.svg)](https://clojars.org/alumbra/pipeline)

[graphql]: http://graphql.org
[alumbra-spec]: https://github.com/alumbra/alumbra.spec

## Usage

### Ring Handler

To create a GraphQL-capable Ring endpoint, you need to supply a series of
[alumbra.spec][alumbra-spec]-compatible components:

```clojure
(require '[alumbra.pipeline :as pipeline])

(pipeline/handler
  {:parser-fn       ...
   :validator-fn    ...
   :canonicalize-fn ...
   :context-fn       (fn [request] (read-auth request))
   :executor-fn      (fn [context canonical-operation] ...)})
```

Note that:

- `:parser-fn` should consume an `InputStream` and return a value conforming to
  either `:alumbra/document` or `:alumbra/parser-errors`,
- `:validator-fn` should consume an `:alumbra/document` and return either `nil` or
  a value conforming to `:alumbra/validation-errors`,
- `:canonicalize-fn` should consume a validated `:alumbra/document` and return a
  value conforming to `:alumbra/canonical-document`,
- `:context-fn` should consume a Ring request map and produce any value
  representing the context of the GraphQL query,
- `:executor-fn` should consume the value produced by `:context`, as well as an
  `:alumbra/canonical-operation`, and produce the resolved value as a map.

A variant of this handler that uses pre-defined components can be found in
the main [alumbra][alumbra] repository.

[alumbra]: https://github.com/alumbra/alumbra

### Raw Executor

A low-level query executor can be constructed using `alumbra.pipeline/executor`.
See its docstring for more information.

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
