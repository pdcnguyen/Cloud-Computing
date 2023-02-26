
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package quickstart;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.DynamicEventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.SessionWindowTimeGapExtractor;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.examples.wordcount.util.CLI;
import org.apache.flink.streaming.examples.wordcount.util.WordCountData;
import org.apache.flink.util.Collector;
import org.apache.flink.streaming.api.windowing.*;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.core.fs.FileSystem;

import java.time.Duration;
import java.util.*;


public class LogLevelCount {

    // *************************************************************************
    // PROGRAM
    // *************************************************************************

    public static void main(String[] args) throws Exception {
        final CLI params = CLI.fromArgs(args);

        // Create the execution environment. This is the main entrypoint
        // to building a Flink application.
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setRuntimeMode(params.getExecutionMode());

        // This optional step makes the input parameters
        // available in the Flink UI.
        env.getConfig().setGlobalJobParameters(params);

        DataStream<String> text;
        if (params.getInputs().isPresent()) {
            // Create a new file source that will read files from a given set of directories.
            // Each file will be processed as plain text and split based on newlines.
            FileSource.FileSourceBuilder<String> builder =
                    FileSource.forRecordStreamFormat(
                            new TextLineInputFormat(), params.getInputs().get());

            // If a discovery interval is provided, the source will
            // continuously watch the given directories for new files.
            params.getDiscoveryInterval().ifPresent(builder::monitorContinuously);

            text = env.fromSource(builder.build(), WatermarkStrategy.noWatermarks(), "file-input");
        } else {
            text = env.fromElements(WordCountData.WORDS).name("in-memory-input");
        }

        // count
        DataStream<Tuple2<String, Integer>> counts =
                text.flatMap(new Tokenizer())
                        .name("tokenizer")
                        .keyBy(value -> value.f0)
                        .sum(1)
                        .name("counter");

        // sort
         DataStream<Tuple2<String, Integer>> sortedData = counts
                .windowAll(EventTimeSessionWindows.withGap(Time.seconds(60)))
                .apply(new AllWindowFunction<Tuple2<String, Integer>, Tuple2<String, Integer>, TimeWindow>()
                {
                    @Override
                    public void apply(TimeWindow tw,
                                      Iterable<Tuple2<String, Integer>> input,
                                      Collector<Tuple2<String, Integer>> output) throws Exception
                    {

                        // get highest key value
                        Map<String, Integer> sortedOut = new HashMap<>();
                        for (Tuple2<String, Integer> data : input)
                        {
                            if (!sortedOut.containsKey(data.f0)) {
                                sortedOut.put(data.f0, data.f1);
                            }
                            else if(sortedOut.get(data.f0) < data.f1)
                            {
                                sortedOut.replace(data.f0, data.f1);
                            }
                        }

                        // sort
                        List<Map.Entry<String, Integer>> list = new ArrayList<>(sortedOut.entrySet());
                        list.sort(Collections.reverseOrder(Map.Entry.comparingByValue()));

                        // collect result
                        for (Map.Entry<String, Integer>  entry: list)
                        {
                            output.collect(new Tuple2<>(entry.getKey(), entry.getValue()));
                        }

                    }

                });

        if (params.getOutput().isPresent()) {
            // write as csv to given output filename
            String outputPath = params.getOutput().get().getPath();
           sortedData.writeAsCsv(outputPath, FileSystem.WriteMode.OVERWRITE);
        } else {
            counts.print().name("print-sink");
        }

        // Apache Flink applications are composed lazily. Calling execute
        // submits the Job and begins processing.
        env.execute("quickstart");
    }

    // *************************************************************************
    // USER FUNCTIONS
    // *************************************************************************


    public static final class Tokenizer
            implements FlatMapFunction<String, Tuple2<String, Integer>> {

        @Override
        public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
            // split by '-' and ' '
            String [] tokens = value.split(" - ");
            String [] log = tokens[1].split(" ");
            
            out.collect(new Tuple2<>(log[0], 1));

        }
    }

}
