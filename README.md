# MonkeyCI Maven Plugin

This is a [MonkeyCI](https://monkeyci.com) [plugin](https://docs.monkeyci.com/articles/plugins)
that provides [Maven](https://maven.apache.org) functionality to build scripts.
You could of course also declare your own container jobs, but why should you, if
you can just use this very simple plugin?

## Usage

Include it in your `.monkeyci/deps.edn`:
```clojure
{:deps {com.monkeyci/plugin-mvn {:mvn/version "<VERSION>"}}}
```

And `require` it in your `build.clj`:

```clojure
(ns build
  (:require [monkey.ci.plugin.mvn :as mvn]))
```

The core function is simply called `mvn` and it allows you to declare a job
that runs any Maven command in a container.  For example:

```clojure
;; This runs `mvn verify` in a container
(mvn/mvn {:job-id "verify" :cmd "verify"})
```

Simple as that!  Note that it also declares a cache for the dependency
repository, which speeds up the process significantly.  But since a some of the
Maven commands are used very frequently, I've added them as separate functions:
`test`, `verify`, `install`, `deploy`,...  Read on for more!

You can also specify multiple goals and options in a more structured manner to
the `mvn` function:

```clojure
(mvn/mvn {:job-id "verify"
          :goals ["verify"]
	  :opts ["--threads=10"]
	  :m2-cache "my-cache"})
```
The above will construct a command line that looks like this:

```shell
$ mvn --threads=10 -Dmaven.repo.local=my-cache verify
```

## Available Jobs

A number of functions have been defined that make it easier to run some common
scenarios.  Some declare a single job, others multiple jobs, even with conditions.
Since most of these jobs are simply thin wrappers around the `mvn` function,
they don't take many options.  If you need differing behaviour, it's better
to just call `mvn` directly.

|Function|Maven command|Options|
|---|---|---|
|`verify`|`mvn verify`|`id`, defaults to `mvn-verify`|
|`test`|`mvn test`|`id`, defaults to `mvn-test`|
|`deploy`|`mvn deploy:deploy`|`verify-job-id`, `job-id`|
|`lib`|`mvn verify && mvn deploy:deploy`|Same as `deploy`|

More will be added later.

## Examples

```clojure
;; Verify job
(mvn/verify)

;; Test job with custom id
(mvn/test "my-test-job")

;; Deploy job, depends on test
(mvn/deploy {:verify-job-id "my-test-job"})
```

## License

Copyright (c) 2025 by [Monkey Projects](https://www.monkey-projects.be)

[MIT License](LICENSE)