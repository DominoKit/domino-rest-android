# domino-rest-android

Adding support for [domino-rest](https://github.com/DominoKit/domino-rest) in android applications.

**Please note that the library needs a minimum SDK version 24 and it uses Java 8.**

## Setup

- Adding these two dependencies:

```
implementation 'org.dominokit.android:domino-rest:1.0-rc.4-SNAPSHOT'
compileOnly 'org.dominokit:domino-rest-apt:1.0-rc.4-SNAPSHOT'
```

> To use the snapshot version without building locally, configure the snapshot repository
```
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots"
    }
    maven {
        url "https://repo.vertispan.com/gwt-snapshot"
    }
}
```

- The library uses annotation processing, so you need to enable this in Gradle, one solution can be:
```
android {
    defaultConfig {
        ...
        javaCompileOptions {
            annotationProcessorOptions {
                includeCompileClasspath true
            }
        }
    }
}
```

- Some of the dependencies have files under `META-INF` so we need to set the packaging options for android apk:
```
packagingOptions {
    exclude 'META-INF/gwt/*'
    exclude '**/*.gwt.xml'
}
```

## Usage

#### Initializing the context

First step to start working with domino-rest is to initialize domino-rest context which will inject the implementation needed, the domino-rest context can be initialized with recommended defaults using
 
```
DominoRestConfig.initDefaults();
```

#### Write the pojos

A pojo used in the service definition as a response or request needs to be annotated with `@JSONMapper` in order to generate the JSON mappers for it, we will see later how we can customize this.

```java
@JSONMapper
public class Movie {

    private String name;
    private int rating;
    private String bio;
    private String releaseDate;

    // setters and getters
}
```

#### Write the service definition

To define a rest service create an interface and annotate it with `@RequestFactory` which will trigger the annotation processor when we compile to generate the rest client.
Add as many methods annotated using JaxRs annotations, and the processor will create a request class and a factory method to execute that method and call the server. 


```java
@RequestFactory
public interface MoviesService {

    @Path("library/movies/:movieName")
    @GET
    MovieResponse getMovieByName(String movieName);

    @Path("library/movies")
    @GET
    List<Movie> listMovies();

    @Path("library/movies/:name")
    @PUT
    void updateMovie(@RequestBody Movie movie);
}
```

Any pojo used in the service response or request needs to be annotated with `@JSONMapper` in order to enable JSON serialization/deserialization.

#### Use the generated client

The generated client class will be named with the service interface name + "Factory", get the instance and call the service method :

```java
MoviesServiceFactory.INSTANCE
    .getMovieByName("hulk")
    .onSuccess(movie -> {
        //do something on success
    })
    .onFailed(failedResponse -> {
        //do something on error
    })
    .send();

MoviesServiceFactory.INSTANCE
    .listMovies()
    .onSuccess(movies -> {
        //do something on success
    })
    .onFailed(failedResponse -> {
        //do something on error
    })
    .send();
    
MoviesServiceFactory.INSTANCE
    .updateMovie(movie)
    .onSuccess(aVoid -> {
        //do something on success
    })
    .onFailed(failedResponse -> {
        //do something on error
    })
    .send();

```


#### for other features, please refer to [the main documentation for domino-rest](https://github.com/DominoKit/domino-rest)
