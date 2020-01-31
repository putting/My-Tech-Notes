# Persisting a file loacally

## Kyro Serialization
This is used by Spark.
It overs roughly 80% smaller serialization (with compression) size than java serialization.

### Written using GZIPOutputStream and read using iterator on GZIPInputStream. Both wrapped in kyro

```java
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.mercuria.icts.poller.key.PrimaryKey;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import org.apache.commons.lang3.mutable.MutableLong;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BufferInFile<T> {

    private final Kryo kryo;
    private final Class<? extends T> clazz;
    private final Logger logger;
    private final String table;
    private final MutableLong counter = new MutableLong(0L);
    private Path tempFile;

    public static <T> BufferInFile<T> init(Class<? extends T> clazz, Iterator<T> iterator, Logger logger, String table) throws IOException {
        BufferInFile<T> tBufferInFile = new BufferInFile<T>(clazz, logger, table);
        tBufferInFile.writeToFile(iterator);
        return tBufferInFile;
    }

    private BufferInFile(Class<? extends T> clazz, Logger logger, String table) {
        this.clazz = clazz;
        this.logger = logger;
        this.table = table;
        this.kryo = new Kryo();
        kryo.register(clazz);
        kryo.register(PrimaryKey.class);
        UnmodifiableCollectionsSerializer.registerSerializers(kryo);
        ImmutableMapSerializer.registerSerializers(kryo);
        ImmutableSetSerializer.registerSerializers(kryo);
        ImmutableListSerializer.registerSerializers(kryo);
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    private Path writeToFile(Iterator<T> iterator) throws IOException {
        this.tempFile = Files.createTempFile(table + "_" + Instant.now().toEpochMilli() + "_snap", ".dat");
        logger.debug("Will cache data for {} in file {}", this.table, tempFile);

        Instant startTime = Instant.now();
        try (UnsafeOutput output = new UnsafeOutput(new GZIPOutputStream(Files.newOutputStream(tempFile)))) {
            iterator
                    .forEachRemaining(entityVersion -> {
                        kryo.writeObject(output, entityVersion);
                        counter.increment();
                    });

            logger.debug("Written {} entity versions for {} to file {}", counter.longValue(), this.table, tempFile);
        }
        logger.debug("Time taken to buffer results for {} is {}s", table, Duration.between(startTime, Instant.now()).getSeconds());
        return tempFile;

    }

    public CloseableIterator<T> resultIterator() throws IOException {
        UnsafeInput input = new UnsafeInput(new GZIPInputStream(Files.newInputStream(tempFile)));
        return new CloseableIterator<T>() {
            @Override
            public void close() throws IOException {
                try {
                    input.close();
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }

            @Override
            public boolean hasNext() {
                boolean hasMore = !input.eof();
                if(!hasMore){
                    if (counter.longValue() != 0) {
                        logger.error("Remaining {} entity versions for {} from file {}", counter.longValue(), table, tempFile);
                    } else {
                        logger.debug("Consumed all entity versions for {} from file {}", table, tempFile);
                    }
                }
                return hasMore;
            }

            @Override
            public T next() {
                T readObject = kryo.readObject(input, clazz);
                counter.decrement();
                return readObject;
            }
        };
    }
}
```
