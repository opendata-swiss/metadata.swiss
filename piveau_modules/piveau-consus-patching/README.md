# Pipe Module for workarounds

This is a custom piveau pipe module. It's used to apply some workarounds in pipes for custom resources (eg. showcases)

Only works on `NTRIPLES` I/O.

## Instructions

Build it 
```
$ docker build -t piveau-consus-patching .
```


## Configuration

### Pipe

#### _optional_

* `actions`

  Contains an array of actions
  Supported actions are:
    * `remove-dataset-cloak`: removes any triples matching `?s a dcat:Dataset .`
    * `signal-resource-*`: signals to subsequent segments the value of `resourceType` 
       * for example, `signal-resource-showcase` signals that `resourceType` is "showcase"


### Logging

| Variable                   | Description                                               | Default Value                           |
|:---------------------------|:----------------------------------------------------------|:----------------------------------------|
| `PIVEAU_PIPE_LOG_LEVEL`    | The log level for the pipe context (eg. `DEBUG`, `TRACE`) | `"INFO"`                                |
