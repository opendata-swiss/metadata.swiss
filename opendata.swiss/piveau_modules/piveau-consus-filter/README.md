# Pipe Module for workarounds

This is a custom piveau pipe module. It's used to filter out datasets.

Only works on `NTRIPLES` I/O.

## Instructions

Build it 
```
$ docker build -t piveau-consus-filter .
```


## Configuration

### Pipe


### Logging

| Variable                   | Description                                               | Default Value                           |
|:---------------------------|:----------------------------------------------------------|:----------------------------------------|
| `PIVEAU_PIPE_LOG_LEVEL`    | The log level for the pipe context (eg. `DEBUG`, `TRACE`) | `"INFO"`                                |
