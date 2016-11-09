# alumbra.ring

This library provides a Ring handler implementation for GraphQL query execution.
It uses pluggable components that consume/produce data structures as described
in [alumbra.spec][alumbra-spec].

[alumbra-spec]: https://github.com/alumbra/alumbra.spec

## Usage

Don't use it yet – but here's the vision:

```clojure
(require '[alumbra.ring :as graphql-ring])

(def app
  (graphql-ring/handler
    {:schema   "type Person { ... } ..."
     :context  (fn [request] (read-auth request))
     :executor (fn [context canonical-operation] ...)}))

(defonce server
  (start-server #'app {:port 8080}))
```

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
