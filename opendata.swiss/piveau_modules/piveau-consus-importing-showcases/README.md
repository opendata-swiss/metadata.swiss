# Pipe Module for importing Showcases

This is a custom piveau pipe module. It's used to import showcases from the app (dcap)


## Instructions

Build it 
```
$ docker build -t piveau-consus-importing-showcases .
```


## Configuration

### Pipe

#### _mandatory_

* `address`

  Configuration of the API endpoint


* `catalogue`

  The id of the target catalogue


### Logging

| Variable                   | Description                                               | Default Value                           |
|:---------------------------|:----------------------------------------------------------|:----------------------------------------|
| `PIVEAU_PIPE_LOG_LEVEL`    | The log level for the pipe context (eg. `DEBUG`, `TRACE`) | `"INFO"`                                |
