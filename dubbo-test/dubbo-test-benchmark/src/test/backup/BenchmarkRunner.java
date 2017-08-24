package com.dubbo.serialize.benchmark;

import serializers.jackson.*;
import serializers.json.FastJSONDatabind;
import serializers.json.FlexjsonDatabind;
import serializers.json.JsonArgoTree;
import serializers.json.JsonDotOrgManualTree;
import serializers.json.JsonGsonDatabind;
import serializers.json.JsonGsonManual;
import serializers.json.JsonGsonTree;
import serializers.json.JsonLibJsonDatabind;
import serializers.json.JsonPathDeserializerOnly;
import serializers.json.JsonSimpleManualTree;
import serializers.json.JsonSimpleWithContentHandler;
import serializers.json.JsonSmartManualTree;
import serializers.json.JsonSvensonDatabind;
import serializers.json.JsonTwoLattes;
import serializers.json.JsonijJpath;
import serializers.json.JsonijManualTree;
import serializers.protostuff.Protostuff;
import serializers.protostuff.ProtostuffJson;
import serializers.protostuff.ProtostuffSmile;
import serializers.xml.XmlJavolution;
import serializers.xml.XmlStax;
import serializers.xml.XmlXStream;

import java.net.URLEncoder;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;

public class BenchmarkRunner {
    public final static int DEFAULT_ITERATIONS = 2000;
    public final static int DEFAULT_TRIALS = 20;

    /**
     * Number of milliseconds to warm up for each operation type for each serializer. Let's
     * start with 3 seconds.
     */
    final static long DEFAULT_WARMUP_MSECS = 3000;

    // These tests aren't included by default.  Use the "-hidden" flag to enable them.
    private static final HashSet<String> HIDDEN = new HashSet<String>();
    private static final String ERROR_DIVIDER = "-------------------------------------------------------------------";
    private static final TestCase Create = new TestCase() {
        public <J> double run(Transformer<J, Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception {
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                transformer.forward(value);
            }
            return iterationTime(System.nanoTime() - start, iterations);
        }
    };
    private static final TestCase Serialize = new TestCase() {
        public <J> double run(Transformer<J, Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception {
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                Object obj = transformer.forward(value);
                serializer.serialize(obj);
            }
            return iterationTime(System.nanoTime() - start, iterations);
        }
    };
    private static final TestCase SerializeSameObject = new TestCase() {
        public <J> double run(Transformer<J, Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception {
            // let's reuse same instance to reduce overhead
            Object obj = transformer.forward(value);
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                serializer.serialize(obj);
                //if (i % 1000 == 0)
                //	doGc();
            }
            return iterationTime(System.nanoTime() - start, iterations);
        }
    };

    // ------------------------------------------------------------------------------------
    private static final TestCase Deserialize = new TestCase() {
        public <J> double run(Transformer<J, Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception {
            byte[] array = serializer.serialize(transformer.forward(value));
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                serializer.deserialize(array);
            }
            return iterationTime(System.nanoTime() - start, iterations);
        }
    };

    // ------------------------------------------------------------------------------------
    private static final TestCase DeserializeAndCheck = new TestCase() {
        public <J> double run(Transformer<J, Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception {
            byte[] array = serializer.serialize(transformer.forward(value));
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                Object obj = serializer.deserialize(array);
                transformer.reverse(obj);
            }
            return iterationTime(System.nanoTime() - start, iterations);
        }
    };
    private static final TestCase DeserializeAndCheckShallow = new TestCase() {
        public <J> double run(Transformer<J, Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception {
            byte[] array = serializer.serialize(transformer.forward(value));
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                Object obj = serializer.deserialize(array);
                transformer.shallowReverse(obj);
            }
            return iterationTime(System.nanoTime() - start, iterations);
        }
    };

    static {
        // CKS is not included because it's not really publicly released.
        HIDDEN.add("cks");
        HIDDEN.add("cks-text");
    }

    public static void main(String[] args) {
        // --------------------------------------------------
        // Parse command-line options.

        Boolean filterIsInclude = null;
        Set<String> filterStrings = null;
        Integer iterations = null;
        Integer trials = null;
        Long warmupTime = null;
        boolean printChart = false;
        boolean prewarm = false;
        String dataFileName = null;
        boolean enableHidden = false;

        Set<String> optionsSeen = new HashSet<String>();

        for (String arg : args) {
            String remainder;
            if (arg.startsWith("--")) {
                remainder = arg.substring(2);
            } else if (arg.startsWith("-")) {
                remainder = arg.substring(1);
            } else if (dataFileName == null) {
                dataFileName = arg;
                continue;
            } else {
                System.err.println("Expecting only one non-option argument (<data-file> = \"" + dataFileName + "\").");
                System.err.println("Found a second one: \"" + arg + "\"");
                System.err.println("Use \"-help\" for usage information.");
                System.exit(1);
                return;
            }

            String option, value;
            int eqPos = remainder.indexOf('=');
            if (eqPos >= 0) {
                option = remainder.substring(0, eqPos);
                value = remainder.substring(eqPos + 1);
            } else {
                option = remainder;
                value = null;
            }

            if (!optionsSeen.add(option)) {
                System.err.println("Repeated option: \"" + arg + "\"");
                System.exit(1);
                return;
            }

            if (option.equals("include")) {
                if (value == null) {
                    System.err.println("The \"include\" option requires a value.");
                    System.exit(1);
                    return;
                }
                if (filterIsInclude == null) {
                    filterIsInclude = true;
                    filterStrings = new HashSet<String>(Arrays.asList(value.split(",")));
                } else {
                    System.err.println("Can't use 'include' and 'exclude' options at the same time.");
                    System.exit(1);
                    return;
                }
            } else if (option.equals("exclude")) {
                if (value == null) {
                    System.err.println("The \"exclude\" option requires a value.");
                    System.exit(1);
                    return;
                }
                if (filterIsInclude == null) {
                    filterIsInclude = false;
                    filterStrings = new HashSet<String>(Arrays.asList(value.split(",")));
                } else {
                    System.err.println("Can't use 'include' and 'exclude' options at the same time.");
                    System.exit(1);
                    return;
                }
            } else if (option.equals("iterations")) {
                if (value == null) {
                    System.err.println("The \"iterations\" option requires a value.");
                    System.exit(1);
                    return;
                }
                assert iterations == null;
                try {
                    iterations = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid value for \"iterations\" option: \"" + value + "\"");
                    System.exit(1);
                    return;
                }
                if (iterations < 1) {
                    System.err.println("Invalid value for \"iterations\" option: \"" + value + "\"");
                    System.exit(1);
                    return;
                }
            } else if (option.equals("trials")) {
                if (value == null) {
                    System.err.println("The \"trials\" option requires a value.");
                    System.exit(1);
                    return;
                }
                assert trials == null;
                try {
                    trials = Integer.parseInt(value);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid value for \"trials\" option: \"" + value + "\"");
                    System.exit(1);
                    return;
                }
                if (trials < 1) {
                    System.err.println("Invalid value for \"trials\" option: \"" + value + "\"");
                    System.exit(1);
                    return;
                }
            } else if (option.equals("warmup-time")) {
                if (value == null) {
                    System.err.println("The \"warmup-time\" option requires a value.");
                    System.exit(1);
                    return;
                }
                assert warmupTime == null;
                try {
                    warmupTime = Long.parseLong(value);
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid value for \"warmup-time\" option: \"" + value + "\"");
                    System.exit(1);
                    return;
                }
                if (warmupTime < 0) {
                    System.err.println("Invalid value for \"warmup-time\" option: \"" + value + "\"");
                    System.exit(1);
                    return;
                }
            } else if (option.equals("pre-warmup")) {
                if (value != null) {
                    System.err.println("The \"pre-warmup\" option does not take a value: \"" + arg + "\"");
                    System.exit(1);
                    return;
                }
                assert !prewarm;
                prewarm = true;
            } else if (option.equals("chart")) {
                if (value != null) {
                    System.err.println("The \"chart\" option does not take a value: \"" + arg + "\"");
                    System.exit(1);
                    return;
                }
                assert !printChart;
                printChart = true;
            } else if (option.equals("hidden")) {
                if (value != null) {
                    System.err.println("The \"hidden\" option does not take a value: \"" + arg + "\"");
                    System.exit(1);
                    return;
                }
                assert !enableHidden;
                enableHidden = true;
            } else if (option.equals("help")) {
                if (value != null) {
                    System.err.println("The \"help\" option does not take a value: \"" + arg + "\"");
                    System.exit(1);
                    return;
                }
                if (args.length != 1) {
                    System.err.println("The \"help\" option cannot be combined with any other option.");
                    System.exit(1);
                    return;
                }

                System.out.println();
                System.out.println("Usage: run [options] <data-file>");
                System.out.println();
                System.out.println("Options:");
                System.out.println("  -iterations=n         [default=" + DEFAULT_ITERATIONS + "]");
                System.out.println("  -trials=n             [default=" + DEFAULT_TRIALS + "]");
                System.out.println("  -warmup-time=millis   [default=" + DEFAULT_WARMUP_MSECS + "]");
                System.out.println("  -pre-warmup           (warm all serializers before the first measurement)");
                System.out.println("  -chart                (generate a Google Chart URL for the results)");
                System.out.println("  -include=impl1,impl2,impl3,...");
                System.out.println("  -exclude=impl1,impl2,impl3,...");
                System.out.println("  -hidden               (enable \"hidden\" serializers)");
                System.out.println("  -help");
                System.out.println();
                System.out.println("Example: run  -chart -include=protobuf,thrift  data/media.1.cks");
                System.out.println();
                System.exit(0);
                return;
            } else {
                System.err.println("Unknown option: \"" + arg + "\"");
                System.err.println("Use \"-help\" for usage information.");
                System.exit(1);
                return;
            }
        }

        if (iterations == null) iterations = DEFAULT_ITERATIONS;
        if (trials == null) trials = DEFAULT_TRIALS;
        if (warmupTime == null) warmupTime = DEFAULT_WARMUP_MSECS;

        if (dataFileName == null) {
            System.err.println("Missing <data-file> argument.");
            System.err.println("Use \"-help\" for usage information.");
            System.exit(1);
            return;
        }

        // --------------------------------------------------
        // Load serializers.

        TestGroups groups = new TestGroups();

        // Binary Formats; language-specific ones
        JavaBuiltIn.register(groups);
        JavaManual.register(groups);
        Scala.register(groups);
        // hessian and kryo are Java object serializations
        Hessian.register(groups);
        Dubbo.register(groups);
        Kryo.register(groups);
        Wobly.register(groups);

        // Binary formats, generic: protobuf, thrift, avro, kryo, CKS, msgpack
        Protobuf.register(groups);
        ActiveMQProtobuf.register(groups);
        Protostuff.register(groups);
        Thrift.register(groups);
        AvroSpecific.register(groups);
        AvroGeneric.register(groups);
        CksBinary.register(groups);
        MsgPack.register(groups);

        // JSON
        JacksonJsonManual.register(groups);
        JacksonJsonTree.register(groups);
        JacksonJsonTreeWithStrings.register(groups);
        JacksonJsonDatabind.register(groups);
        JacksonJsonDatabindWithStrings.register(groups);
        JsonTwoLattes.register(groups);
        ProtostuffJson.register(groups);
        ProtobufJson.register(groups);
        JsonGsonManual.register(groups);
        JsonGsonTree.register(groups);
        JsonGsonDatabind.register(groups);
        JsonSvensonDatabind.register(groups);
        FlexjsonDatabind.register(groups);
        JsonLibJsonDatabind.register(groups);
        FastJSONDatabind.register(groups);
        JsonSimpleWithContentHandler.register(groups);
        JsonSimpleManualTree.register(groups);
        JsonSmartManualTree.register(groups);
        JsonDotOrgManualTree.register(groups);
        JsonijJpath.register(groups);
        JsonijManualTree.register(groups);
        JsonArgoTree.register(groups);
        JsonPathDeserializerOnly.register(groups);
        // Then JSON-like
        // CKS text is textual JSON-like format
        CksText.register(groups);
        // then binary variants
        // BSON is binary JSON-like format
        JacksonBsonManual.register(groups);
        JacksonBsonDatabind.register(groups);
        MongoDB.register(groups);
        // Smile is 1-to-1 binary representation of JSON
        JacksonSmileManual.register(groups);
        JacksonSmileDatabind.register(groups);
        ProtostuffSmile.register(groups);

        // XML-based formats.
        XmlStax.register(groups);
        XmlXStream.register(groups);
        JacksonXmlDatabind.register(groups);
        XmlJavolution.register(groups);

        // --------------------------------------------------
        // Load data value.

        Object dataValue;
        TestGroup<?> group;
        {
            File dataFile = new File(dataFileName);
            if (!dataFile.exists()) {
                System.out.println("Couldn't find data file \"" + dataFile.getPath() + "\"");
                System.exit(1);
                return;
            }

            String[] parts = dataFile.getName().split("\\.");
            if (parts.length < 3) {
                System.out.println("Data file \"" + dataFile.getName() + "\" should be of the form \"<type>.<name>.<extension>\"");
                System.exit(1);
                return;
            }

            String dataType = parts[0];
            String extension = parts[parts.length - 1];

            group = groups.groupMap.get(dataType);
            if (group == null) {
                System.out.println("Data file \"" + dataFileName + "\" can't be loaded.");
                System.out.println("Don't know about data type \"" + dataType + "\"");
                System.exit(1);
                return;
            }

            TestGroup.Entry<?, Object> loader = group.extensionMap.get(parts[parts.length - 1]);
            if (loader == null) {
                System.out.println("Data file \"" + dataFileName + "\" can't be loaded.");
                System.out.println("No deserializer registered for data type \"" + dataType + "\" and file extension \"." + extension + "\"");
                System.exit(1);
                return;
            }


            Object deserialized;
            try {
                byte[] fileBytes = readFile(new File(dataFileName)); // Load entire file into a byte array.
                deserialized = loader.serializer.deserialize(fileBytes);
            } catch (Exception ex) {
                System.err.println("Error loading data from file \"" + dataFileName + "\".");
                System.err.println(ex.getMessage());
                System.exit(1);
                return;
            }

            dataValue = loader.transformer.reverse(deserialized);
        }

        @SuppressWarnings("unchecked")
        TestGroup<Object> group_ = (TestGroup<Object>) group;

        // --------------------------------------------------

        Set<String> matched = new HashSet<String>();

        Iterable<TestGroup.Entry<Object, Object>> available;

        if (enableHidden) {
            // Use all of them.
            available = group_.entries;
        } else {
            // Remove the hidden ones.
            ArrayList<TestGroup.Entry<Object, Object>> unhidden = new ArrayList<TestGroup.Entry<Object, Object>>();
            for (TestGroup.Entry<?, Object> entry_ : group.entries) {
                @SuppressWarnings("unchecked")
                TestGroup.Entry<Object, Object> entry = (TestGroup.Entry<Object, Object>) entry_;
                String name = entry.serializer.getName();
                if (!HIDDEN.contains(name)) unhidden.add(entry);
            }
            available = unhidden;
        }

        Iterable<TestGroup.Entry<Object, Object>> matchingEntries;
        if (filterStrings == null) {
            matchingEntries = available;
        } else {
            ArrayList<TestGroup.Entry<Object, Object>> al = new ArrayList<TestGroup.Entry<Object, Object>>();
            matchingEntries = al;

            for (TestGroup.Entry<?, Object> entry_ : available) {
                @SuppressWarnings("unchecked")
                TestGroup.Entry<Object, Object> entry = (TestGroup.Entry<Object, Object>) entry_;

                String name = entry.serializer.getName();

                // See if any of the filters match.
                boolean found = false;
                for (String s : filterStrings) {
                    boolean thisOneMatches = match(s, name);
                    if (thisOneMatches) {
                        matched.add(s);
                        found = true;
                    }
                }

                if (found == filterIsInclude) {
                    al.add(entry);
                }
            }

            Set<String> unmatched = new HashSet<String>(filterStrings);
            unmatched.removeAll(matched);
            for (String s : unmatched) {
                System.err.println("Warning: there is no implementation name matching the pattern \"" + s + "\"");

                if (!enableHidden) {
                    for (String hiddenName : HIDDEN) {
                        if (match(s, hiddenName)) {
                            System.err.println("(The \"" + hiddenName + "\", serializer is hidden by default.");
                            System.err.println(" Use the \"-hidden\" option to enable hidden serializers)");
                            break;
                        }
                    }
                }
            }
        }

        EnumMap<measurements, Map<String, Double>> values;
        StringWriter errors = new StringWriter();
        PrintWriter errorsPW = new PrintWriter(errors);
        try {
            values = start(errorsPW, iterations, trials, warmupTime, prewarm, matchingEntries, dataValue);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            System.exit(1);
            return;
        }

        if (printChart) {
            printImages(values);
        }

        // Print errors after chart.  That way you can't miss it.
        String errorsString = errors.toString();
        if (errorsString.length() > 0) {
            System.out.println(ERROR_DIVIDER);
            System.out.println("Errors occurred during benchmarking:");
            System.out.print(errorsString);
            System.exit(1);
            return;
        }
    }

    private static boolean match(String pattern, String name) {
        StringBuilder regex = new StringBuilder();

        while (pattern.length() > 0) {
            int starPos = pattern.indexOf('*');
            if (starPos < 0) {
                regex.append(Pattern.quote(pattern));
                break;
            } else {
                String beforeStar = pattern.substring(0, starPos);
                String afterStar = pattern.substring(starPos + 1);

                regex.append(Pattern.quote(beforeStar));
                regex.append(".*");
                pattern = afterStar;
            }
        }

        return Pattern.matches(regex.toString(), name);
    }

    private static byte[] readFile(File file)
            throws IOException {
        FileInputStream fin = new FileInputStream(file);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            byte[] data = new byte[1024];
            while (true) {
                int numBytes = fin.read(data);
                if (numBytes < 0) break;
                baos.write(data, 0, numBytes);
            }
            return baos.toByteArray();
        } finally {
            fin.close();
        }
    }

    private static double iterationTime(long delta, int iterations) {
        return (double) delta / (double) (iterations);
    }

    /**
     * JVM is not required to honor GC requests, but adding bit of sleep around request is
     * most likely to give it a chance to do it.
     */
    private static void doGc() {
        try {
            Thread.sleep(50L);
        } catch (InterruptedException ie) {
            System.err.println("Interrupted while sleeping in serializers.BenchmarkRunner.doGc()");
        }
        System.gc();
        try { // longer sleep afterwards (not needed by GC, but may help with scheduling)
            Thread.sleep(200L);
        } catch (InterruptedException ie) {
            System.err.println("Interrupted while sleeping in serializers.BenchmarkRunner.doGc()");
        }
    }

    // ------------------------------------------------------------------------------------

    private static <J> EnumMap<measurements, Map<String, Double>>
    start(PrintWriter errors, int iterations, int trials, long warmupTime, boolean prewarm, Iterable<TestGroup.Entry<J, Object>> groups, J value) throws Exception {
        // Check correctness first.
        System.out.println("Checking correctness...");
        for (TestGroup.Entry<J, Object> entry : groups) {
            checkCorrectness(errors, entry.transformer, entry.serializer, value);
        }
        System.out.println("[done]");

        // Pre-warm.
        if (prewarm) {
            System.out.print("Pre-warmup...");
            for (TestGroup.Entry<J, Object> entry : groups) {
                TestCaseRunner<J> runner = new TestCaseRunner<J>(entry.transformer, entry.serializer, value);
                String name = entry.serializer.getName();
                System.out.print(" " + name);

                warmCreation(runner, warmupTime);
                warmSerialization(runner, warmupTime);
                warmDeserialization(runner, warmupTime);
            }
            System.out.println();
            System.out.println("[done]");
        }

        System.out.printf("%-32s %6s %7s %7s %7s %7s %7s %7s %6s %5s\n",
                "",
                "create",
                "ser",
                "+same",
                "deser",
                "+shal",
                "+deep",
                "total",
                "size",
                "+dfl");
        EnumMap<measurements, Map<String, Double>> values = new EnumMap<measurements, Map<String, Double>>(measurements.class);
        for (measurements m : measurements.values())
            values.put(m, new HashMap<String, Double>());

        // Actual tests.
        for (TestGroup.Entry<J, Object> entry : groups) {
            TestCaseRunner<J> runner = new TestCaseRunner<J>(entry.transformer, entry.serializer, value);
            String name = entry.serializer.getName();
            try {

				/*
                 * Should only warm things for the serializer that we test next: HotSpot JIT will
				 * otherwise spent most of its time optimizing slower ones... Use
				 * -XX:CompileThreshold=1 to hint the JIT to start immediately
				 *
				 * Actually: 1 is often not a good value -- threshold is the number
				 * of samples needed to trigger inlining, and there's no point in
				 * inlining everything. Default value is in thousands, so lowering
				 * it to, say, 1000 is usually better.
				 */
                warmCreation(runner, warmupTime);

                doGc();
                double timeCreate = runner.runTakeMin(trials, Create, iterations * 100); // do more iteration for object creation because of its short time

                warmSerialization(runner, warmupTime);

                doGc();
                double timeSerializeDifferentObjects = runner.runTakeMin(trials, Serialize, iterations);

                doGc();
                double timeSerializeSameObject = runner.runTakeMin(trials, SerializeSameObject, iterations);

                warmDeserialization(runner, warmupTime);

                doGc();
                double timeDeserializeNoFieldAccess = runner.runTakeMin(trials, Deserialize, iterations);

                doGc();
                double timeDeserializeAndCheckShallow = runner.runTakeMin(trials, DeserializeAndCheckShallow, iterations);

                doGc();
                double timeDeserializeAndCheck = runner.runTakeMin(trials, DeserializeAndCheck, iterations);

                double totalTime = timeSerializeDifferentObjects + timeDeserializeAndCheck;

                byte[] array = entry.serializer.serialize(entry.transformer.forward(value));

                byte[] compressDeflate = compressDeflate(array);

                System.out.printf("%-32s %6.0f %7.0f %7.0f %7.0f %7.0f %7.0f %7.0f %6d %5d\n",
                        name,
                        timeCreate,
                        timeSerializeDifferentObjects,
                        timeSerializeSameObject,
                        timeDeserializeNoFieldAccess,
                        timeDeserializeAndCheckShallow,
                        timeDeserializeAndCheck,
                        totalTime,
                        array.length,
                        compressDeflate.length);

                addValue(values, name, timeCreate, timeSerializeDifferentObjects, timeSerializeSameObject,
                        timeDeserializeNoFieldAccess, timeDeserializeAndCheckShallow, timeDeserializeAndCheck, totalTime,
                        array.length, compressDeflate.length);
            } catch (Exception ex) {
                System.out.println("ERROR: \"" + name + "\" crashed during benchmarking.");
                errors.println(ERROR_DIVIDER);
                errors.println("\"" + name + "\" crashed during benchmarking.");
                ex.printStackTrace(errors);
            }
        }

        return values;
    }

    private static byte[] compressDeflate(byte[] data) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(500);
            DeflaterOutputStream compresser = new DeflaterOutputStream(bout);
            compresser.write(data, 0, data.length);
            compresser.finish();
            compresser.flush();
            return bout.toByteArray();
        } catch (IOException ex) {
            AssertionError ae = new AssertionError("IOException while writing to ByteArrayOutputStream!");
            ae.initCause(ex);
            throw ae;
        }
    }

    /**
     * Method that tries to validate correctness of serializer, using
     * round-trip (construct, serializer, deserialize; compare objects
     * after steps 1 and 3).
     */
    private static <J> void checkCorrectness(PrintWriter errors, Transformer<J, Object> transformer, Serializer<Object> serializer, J value)
            throws Exception {
        Object specialInput;
        String name = serializer.getName();

        try {
            specialInput = transformer.forward(value);
        } catch (Exception ex) {
            System.out.println("ERROR: \"" + name + "\" crashed during forward transformation.");
            errors.println(ERROR_DIVIDER);
            errors.println("\"" + name + "\" crashed during forward transformation.");
            ex.printStackTrace(errors);
            return;
        }

        byte[] array;

        try {
            array = serializer.serialize(specialInput);
        } catch (Exception ex) {
            System.out.println("ERROR: \"" + name + "\" crashed during serialization.");
            errors.println(ERROR_DIVIDER);
            errors.println("\"" + name + "\" crashed during serialization.");
            ex.printStackTrace(errors);
            return;
        }

        Object specialOutput;

        try {
            specialOutput = serializer.deserialize(array);
        } catch (Exception ex) {
            System.out.println("ERROR: \"" + name + "\" crashed during deserialization.");
            errors.println(ERROR_DIVIDER);
            errors.println("\"" + name + "\" crashed during deserialization.");
            ex.printStackTrace(errors);
            return;
        }

        J output;
        try {
            output = transformer.reverse(specialOutput);
        } catch (Exception ex) {
            System.out.println("ERROR: \"" + name + "\" crashed during reverse transformation.");
            errors.println(ERROR_DIVIDER);
            errors.println("\"" + name + "\" crashed during reverse transformation.");
            ex.printStackTrace(errors);
            return;
        }


        if (!value.equals(output)) {
            System.out.println("ERROR: \"" + name + "\" failed round-trip check.");
            errors.println(ERROR_DIVIDER);
            errors.println("\"" + name + "\" failed round-trip check.");
            errors.println("ORIGINAL:  " + value);
            errors.println("ROUNDTRIP: " + output);
        }
    }

    private static void printImages(EnumMap<measurements, Map<String, Double>> values) {
        for (measurements m : values.keySet()) {
            Map<String, Double> map = values.get(m);
            ArrayList<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(map.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                    double diff = o1.getValue() - o2.getValue();
                    return diff > 0 ? 1 : (diff < 0 ? -1 : 0);
                }
            });
            LinkedHashMap<String, Double> sortedMap = new LinkedHashMap<String, Double>();
            for (Map.Entry<String, Double> entry : list) {
                if (!entry.getValue().isNaN()) {
                    sortedMap.put(entry.getKey(), entry.getValue());
                }
            }
            if (!sortedMap.isEmpty()) printImage(sortedMap, m);
        }
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printImage(Map<String, Double> map, measurements m) {
        StringBuilder valSb = new StringBuilder();
        String names = "";
        double max = Double.MIN_NORMAL;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            double value = entry.getValue();
            valSb.append((int) value).append(',');
            max = Math.max(max, entry.getValue());
            names = urlEncode(entry.getKey()) + '|' + names;
        }

        int headerSize = 30;

        int maxPixels = 300 * 1000; // Limit set by Google's Chart API.

        int maxHeight = 600;
        int width = maxPixels / maxHeight;

        int barThickness = 10;
        int barSpacing = 10;

        int height;

        // Reduce bar thickness and spacing until we can fit in the maximum height.
        while (true) {
            height = headerSize + map.size() * (barThickness + barSpacing);
            if (height <= maxHeight) break;
            barSpacing--;
            if (barSpacing == 1) break;

            height = headerSize + map.size() * (barThickness + barSpacing);
            if (height <= maxHeight) break;
            barThickness--;
            if (barThickness == 1) break;
        }

        boolean truncated = false;
        if (height > maxHeight) {
            truncated = true;
            height = maxHeight;
        }

        double scale = max * 1.1;
        System.out.println("<img src='https://chart.googleapis.com/chart?chtt="
                + urlEncode(m.displayName)
                + "&chf=c||lg||0||FFFFFF||1||76A4FB||0|bg||s||EFEFEF&chs=" + width + "x" + height + "&chd=t:"
                + valSb.toString().substring(0, valSb.length() - 1)
                + "&chds=0," + scale
                + "&chxt=y"
                + "&chxl=0:|" + names.substring(0, names.length() - 1)
                + "&chm=N *f*,000000,0,-1,10&lklk&chdlp=t&chco=660000|660033|660066|660099|6600CC|6600FF|663300|663333|663366|663399|6633CC|6633FF|666600|666633|666666&cht=bhg&chbh=" + barThickness + ",0," + barSpacing + "&nonsense=aaa.png'/>");

        if (truncated) {
            System.err.println("WARNING: Not enough room to fit all bars in chart.");
        }
    }

    private static void addValue(
            EnumMap<measurements, Map<String, Double>> values,
            String name,
            double timeCreate,
            double timeSerializeDifferentObjects,
            double timeSerializeSameObject,
            double timeDeserializeNoFieldAccess,
            double timeDeserializeAndCheckShallow,
            double timeDeserializeAndCheck,
            double totalTime,
            double length, double lengthDeflate) {
        values.get(measurements.timeSerializeDifferentObjects).put(name, timeSerializeDifferentObjects);
        values.get(measurements.timeSerializeSameObject).put(name, timeSerializeSameObject);
        values.get(measurements.timeDeserializeNoFieldAccess).put(name, timeDeserializeNoFieldAccess);
        values.get(measurements.timeDeserializeAndCheckShallow).put(name, timeDeserializeAndCheckShallow);
        values.get(measurements.timeDeserializeAndCheck).put(name, timeDeserializeAndCheck);
        values.get(measurements.totalTime).put(name, totalTime);
        values.get(measurements.length).put(name, length);
        values.get(measurements.lengthDeflate).put(name, lengthDeflate);
        values.get(measurements.timeCreate).put(name, timeCreate);
    }

    private static <J> void warmCreation(TestCaseRunner<J> runner, long warmupTime) throws Exception {
        // Instead of fixed counts, let's try to prime by running for N seconds
        long endTime = System.currentTimeMillis() + warmupTime;
        do {
            runner.run(Create, 10);
        }
        while (System.currentTimeMillis() < endTime);
    }

    private static <J> void warmSerialization(TestCaseRunner<J> runner, long warmupTime) throws Exception {
        // Instead of fixed counts, let's try to prime by running for N seconds
        long endTime = System.currentTimeMillis() + warmupTime;
        do {
            runner.run(Serialize, 10);
        }
        while (System.currentTimeMillis() < endTime);
    }

    private static <J> void warmDeserialization(TestCaseRunner<J> runner, long warmupTime) throws Exception {
        // Instead of fixed counts, let's try to prime by running for N seconds
        long endTime = System.currentTimeMillis() + warmupTime;
        do {
            runner.run(DeserializeAndCheck, 10);
        }
        while (System.currentTimeMillis() < endTime);
    }

    enum measurements {
        timeCreate("create (nanos)"), timeSerializeDifferentObjects("ser (nanos)"), timeSerializeSameObject("ser+same (nanos)"),
        timeDeserializeNoFieldAccess("deser (nanos)"), timeDeserializeAndCheck("deser+deep (nanos)"), timeDeserializeAndCheckShallow("deser+shal (nanos)"),
        totalTime("total (nanos)"), length("size (bytes)"), lengthDeflate("size+dfl (bytes)"),;

        public final String displayName;

        measurements(String displayName) {
            this.displayName = displayName;
        }
    }

    private static abstract class TestCase {
        public abstract <J> double run(Transformer<J, Object> transformer, Serializer<Object> serializer, J value, int iterations) throws Exception;
    }

    private static final class TestCaseRunner<J> {
        private final Transformer<J, Object> transformer;
        private final Serializer<Object> serializer;
        private final J value;

        public TestCaseRunner(Transformer<J, Object> transformer, Serializer<Object> serializer, J value) {
            this.transformer = transformer;
            this.serializer = serializer;
            this.value = value;
        }

        public double run(TestCase tc, int iterations) throws Exception {
            return tc.run(transformer, serializer, value, iterations);
        }

        public double runTakeMin(int trials, TestCase tc, int iterations) throws Exception {
            double minTime = Double.MAX_VALUE;
            for (int i = 0; i < trials; i++) {
                double time = tc.run(transformer, serializer, value, iterations);
                minTime = Math.min(minTime, time);
            }
            return minTime;
        }
    }
}
