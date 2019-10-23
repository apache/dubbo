#include "java_generator.h"

#include <algorithm>
#include <iostream>
#include <iterator>
#include <map>
#include <set>
#include <vector>
#include <google/protobuf/compiler/java/java_names.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/descriptor.pb.h>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/io/zero_copy_stream.h>

// Stringify helpers used solely to cast GRPC_VERSION
#ifndef STR
#define STR(s) #s
#endif

#ifndef XSTR
#define XSTR(s) STR(s)
#endif

#ifndef FALLTHROUGH_INTENDED
#define FALLTHROUGH_INTENDED
#endif

namespace java_dubbo_generator {

using google::protobuf::FileDescriptor;
using google::protobuf::ServiceDescriptor;
using google::protobuf::MethodDescriptor;
using google::protobuf::Descriptor;
using google::protobuf::io::Printer;
using google::protobuf::SourceLocation;
using std::to_string;

// java keywords from: https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.9
static std::set<string> java_keywords = {
  "abstract",
  "assert",
  "boolean",
  "break",
  "byte",
  "case",
  "catch",
  "char",
  "class",
  "const",
  "continue",
  "default",
  "do",
  "double",
  "else",
  "enum",
  "extends",
  "final",
  "finally",
  "float",
  "for",
  "goto",
  "if",
  "implements",
  "import",
  "instanceof",
  "int",
  "interface",
  "long",
  "native",
  "new",
  "package",
  "private",
  "protected",
  "public",
  "return",
  "short",
  "static",
  "strictfp",
  "super",
  "switch",
  "synchronized",
  "this",
  "throw",
  "throws",
  "transient",
  "try",
  "void",
  "volatile",
  "while",
  // additional ones added by us
  "true",
  "false",
};

// Adjust a method name prefix identifier to follow the JavaBean spec:
//   - decapitalize the first letter
//   - remove embedded underscores & capitalize the following letter
//  Finally, if the result is a reserved java keyword, append an underscore.
static string MixedLower(const string& word) {
  string w;
  w += tolower(word[0]);
  bool after_underscore = false;
  for (size_t i = 1; i < word.length(); ++i) {
    if (word[i] == '_') {
      after_underscore = true;
    } else {
      w += after_underscore ? toupper(word[i]) : word[i];
      after_underscore = false;
    }
  }
  if (java_keywords.find(w) != java_keywords.end()) {
    return w + "_";
  }
  return w;
}

// Converts to the identifier to the ALL_UPPER_CASE format.
//   - An underscore is inserted where a lower case letter is followed by an
//     upper case letter.
//   - All letters are converted to upper case
static string ToAllUpperCase(const string& word) {
  string w;
  for (size_t i = 0; i < word.length(); ++i) {
    w += toupper(word[i]);
    if ((i < word.length() - 1) && islower(word[i]) && isupper(word[i + 1])) {
      w += '_';
    }
  }
  return w;
}

static inline string LowerMethodName(const MethodDescriptor* method) {
  return MixedLower(method->name());
}

static inline string MethodPropertiesFieldName(const MethodDescriptor* method) {
  return "METHOD_" + ToAllUpperCase(method->name());
}

static inline string MethodPropertiesGetterName(const MethodDescriptor* method) {
  return MixedLower("get_" + method->name() + "_method");
}

static inline string MethodIdFieldName(const MethodDescriptor* method) {
  return "METHODID_" + ToAllUpperCase(method->name());
}

static inline bool ShouldGenerateAsLite(const Descriptor* desc) {
  return false;
}

static inline string MessageFullJavaName(bool nano, const Descriptor* desc) {
  string name = google::protobuf::compiler::java::ClassName(desc);
  if (nano && !ShouldGenerateAsLite(desc)) {
    // XXX: Add "nano" to the original package
    // (https://github.com/grpc/grpc-java/issues/900)
    if (isupper(name[0])) {
      // No java package specified.
      return "nano." + name;
    }
    for (size_t i = 0; i < name.size(); ++i) {
      if ((name[i] == '.') && (i < (name.size() - 1)) && isupper(name[i + 1])) {
        return name.substr(0, i + 1) + "nano." + name.substr(i + 1);
      }
    }
  }
  return name;
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
template <typename ITR>
static void GrpcSplitStringToIteratorUsing(const string& full,
                                       const char* delim,
                                       ITR& result) {
  // Optimize the common case where delim is a single character.
  if (delim[0] != '\0' && delim[1] == '\0') {
    char c = delim[0];
    const char* p = full.data();
    const char* end = p + full.size();
    while (p != end) {
      if (*p == c) {
        ++p;
      } else {
        const char* start = p;
        while (++p != end && *p != c);
        *result++ = string(start, p - start);
      }
    }
    return;
  }

  string::size_type begin_index, end_index;
  begin_index = full.find_first_not_of(delim);
  while (begin_index != string::npos) {
    end_index = full.find_first_of(delim, begin_index);
    if (end_index == string::npos) {
      *result++ = full.substr(begin_index);
      return;
    }
    *result++ = full.substr(begin_index, (end_index - begin_index));
    begin_index = full.find_first_not_of(delim, end_index);
  }
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static void GrpcSplitStringUsing(const string& full,
                             const char* delim,
                             std::vector<string>* result) {
  std::back_insert_iterator< std::vector<string> > it(*result);
  GrpcSplitStringToIteratorUsing(full, delim, it);
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static std::vector<string> GrpcSplit(const string& full, const char* delim) {
  std::vector<string> result;
  GrpcSplitStringUsing(full, delim, &result);
  return result;
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static string GrpcEscapeJavadoc(const string& input) {
  string result;
  result.reserve(input.size() * 2);

  char prev = '*';

  for (string::size_type i = 0; i < input.size(); i++) {
    char c = input[i];
    switch (c) {
      case '*':
        // Avoid "/*".
        if (prev == '/') {
          result.append("&#42;");
        } else {
          result.push_back(c);
        }
        break;
      case '/':
        // Avoid "*/".
        if (prev == '*') {
          result.append("&#47;");
        } else {
          result.push_back(c);
        }
        break;
      case '@':
        // '@' starts javadoc tags including the @deprecated tag, which will
        // cause a compile-time error if inserted before a declaration that
        // does not have a corresponding @Deprecated annotation.
        result.append("&#64;");
        break;
      case '<':
        // Avoid interpretation as HTML.
        result.append("&lt;");
        break;
      case '>':
        // Avoid interpretation as HTML.
        result.append("&gt;");
        break;
      case '&':
        // Avoid interpretation as HTML.
        result.append("&amp;");
        break;
      case '\\':
        // Java interprets Unicode escape sequences anywhere!
        result.append("&#92;");
        break;
      default:
        result.push_back(c);
        break;
    }

    prev = c;
  }

  return result;
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
template <typename DescriptorType>
static string GrpcGetCommentsForDescriptor(const DescriptorType* descriptor) {
  SourceLocation location;
  if (descriptor->GetSourceLocation(&location)) {
    return location.leading_comments.empty() ?
      location.trailing_comments : location.leading_comments;
  }
  return string();
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static std::vector<string> GrpcGetDocLines(const string& comments) {
  if (!comments.empty()) {
    // TODO(kenton):  Ideally we should parse the comment text as Markdown and
    //   write it back as HTML, but this requires a Markdown parser.  For now
    //   we just use <pre> to get fixed-width text formatting.

    // If the comment itself contains block comment start or end markers,
    // HTML-escape them so that they don't accidentally close the doc comment.
    string escapedComments = GrpcEscapeJavadoc(comments);

    std::vector<string> lines = GrpcSplit(escapedComments, "\n");
    while (!lines.empty() && lines.back().empty()) {
      lines.pop_back();
    }
    return lines;
  }
  return std::vector<string>();
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
template <typename DescriptorType>
static std::vector<string> GrpcGetDocLinesForDescriptor(const DescriptorType* descriptor) {
  return GrpcGetDocLines(GrpcGetCommentsForDescriptor(descriptor));
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static void GrpcWriteDocCommentBody(Printer* printer,
                                    const std::vector<string>& lines,
                                    bool surroundWithPreTag) {
  if (!lines.empty()) {
    if (surroundWithPreTag) {
      printer->Print(" * <pre>\n");
    }

    for (size_t i = 0; i < lines.size(); i++) {
      // Most lines should start with a space.  Watch out for lines that start
      // with a /, since putting that right after the leading asterisk will
      // close the comment.
      if (!lines[i].empty() && lines[i][0] == '/') {
        printer->Print(" * $line$\n", "line", lines[i]);
      } else {
        printer->Print(" *$line$\n", "line", lines[i]);
      }
    }

    if (surroundWithPreTag) {
      printer->Print(" * </pre>\n");
    }
  }
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static void GrpcWriteDocComment(Printer* printer, const string& comments) {
  printer->Print("/**\n");
  std::vector<string> lines = GrpcGetDocLines(comments);
  GrpcWriteDocCommentBody(printer, lines, false);
  printer->Print(" */\n");
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
static void GrpcWriteServiceDocComment(Printer* printer,
                                       const ServiceDescriptor* service) {
  // Deviating from protobuf to avoid extraneous docs
  // (see https://github.com/google/protobuf/issues/1406);
  printer->Print("/**\n");
  std::vector<string> lines = GrpcGetDocLinesForDescriptor(service);
  GrpcWriteDocCommentBody(printer, lines, true);
  printer->Print(" */\n");
}

// TODO(nmittler): Remove once protobuf includes javadoc methods in distribution.
void GrpcWriteMethodDocComment(Printer* printer,
                           const MethodDescriptor* method) {
  // Deviating from protobuf to avoid extraneous docs
  // (see https://github.com/google/protobuf/issues/1406);
  printer->Print("/**\n");
  std::vector<string> lines = GrpcGetDocLinesForDescriptor(method);
  GrpcWriteDocCommentBody(printer, lines, true);
  printer->Print(" */\n");
}

enum StubType {
  ASYNC_INTERFACE = 0,
  BLOCKING_CLIENT_INTERFACE = 1,
  FUTURE_CLIENT_INTERFACE = 2,
  BLOCKING_SERVER_INTERFACE = 3,
  ASYNC_CLIENT_IMPL = 4,
  BLOCKING_CLIENT_IMPL = 5,
  FUTURE_CLIENT_IMPL = 6,
  ABSTRACT_CLASS = 7,
};

enum CallType {
  ASYNC_CALL = 0,
  BLOCKING_CALL = 1,
  FUTURE_CALL = 2
};

static void PrintMarshallerStaticBlock(const ServiceDescriptor* service,
                                   std::map<string, string>* vars,
                                   Printer* p) {
   for (int i = 0; i < service->method_count(); ++i) {
       const MethodDescriptor* method = service->method(i);
       (*vars)["input_type"] = google::protobuf::compiler::java::ClassName(method->input_type());
       (*vars)["output_type"] = google::protobuf::compiler::java::ClassName(method->output_type());
       p->Print(
          *vars,
          "private static final AtomicBoolean registered = new AtomicBoolean();\n\n");

       p->Print(
           *vars,
           "private static Class<?> init() {\n"
           "    Class<?> clazz = null;\n"
           "    try {\n"
           "        clazz = Class.forName(DemoServiceDubbo.class.getName());\n"
           "        if (registered.compareAndSet(false, true)) {\n"
           "            $ProtobufUtils$.marshaller(\n"
           "                $input_type$.getDefaultInstance());\n"
           "            $ProtobufUtils$.marshaller(\n"
           "                $output_type$.getDefaultInstance());\n"
           "        }\n"
           "     } catch (ClassNotFoundException e) {\n"
           "        // ignore \n"
           "     }\n"
           "     return clazz;\n"
           "}\n\n");
   }
}

static void PrintDubboInterface(
    const ServiceDescriptor* service,
    std::map<string, string>* vars,
    Printer* p, bool generate_nano) {
    const string service_name = service->name();
    (*vars)["service_name"] = service_name;
    (*vars)["dubbo_interface"] = "I" + service_name;

    p->Print(
        "/**\n "
        "* Code generated for Dubbo\n "
        "*/\n"
    );
    p->Print(
    *vars,
    "public interface $dubbo_interface$ {\n\n"
    "   static Class<?> clazz = init();\n\n");

    for (int i = 0; i < service->method_count(); ++i) {
        const MethodDescriptor* method = service->method(i);
        (*vars)["input_type"] = MessageFullJavaName(generate_nano,
                                                    method->input_type());
        (*vars)["output_type"] = MessageFullJavaName(generate_nano,
                                                     method->output_type());
        (*vars)["lower_method_name"] = LowerMethodName(method);

        // Simple RPC
        p->Print(
            *vars,
            "   $output_type$ $lower_method_name$($input_type$ request);\n\n");
       // Simple Future RPC
        p->Print(
            *vars,
            "   $CompletableFuture$<$output_type$> $lower_method_name$Async(\n  $input_type$ request);\n\n");
//            p->Print(
//            *vars,
//            "default $CompletableFuture$<$output_type$> $lower_method_name$Async(\n"
//            "    $input_type$ request) {\n return CompletableFuture.completedFuture($lower_method_name$(request));\n}\n\n");
        p->Outdent();
    }

    p->Outdent();
    p->Print("  }\n\n");

}

static void PrintService(const ServiceDescriptor* service,
                         std::map<string, string>* vars,
                         Printer* p,
                         bool disable_version) {
  (*vars)["service_name"] = service->name();
  (*vars)["file_name"] = service->file()->name();
  (*vars)["service_class_name"] = ServiceClassName(service);
  (*vars)["grpc_version"] = "";
  #ifdef GRPC_VERSION
  if (!disable_version) {
    (*vars)["grpc_version"] = " (version " XSTR(GRPC_VERSION) ")";
  }
  #endif
  // TODO(nmittler): Replace with WriteServiceDocComment once included by protobuf distro.
  GrpcWriteServiceDocComment(p, service);
  p->Print(
      *vars,
      "@$Generated$(\n"
      "    value = \"by gRPC proto compiler$grpc_version$\",\n"
      "    comments = \"Source: $file_name$\")\n");

  if (service->options().deprecated()) {
    p->Print(*vars, "@$Deprecated$\n");
  }

  p->Print(
      *vars,
      "public final class $service_class_name$ {\n\n");
  p->Indent();

  PrintMarshallerStaticBlock(service, vars, p);

  p->Print(
      *vars,
      "private $service_class_name$() {}\n\n");

  p->Print(
      *vars,
      "public static final String SERVICE_NAME = "
      "\"$Package$$service_name$\";\n\n");

  PrintDubboInterface(service, vars, p, false);

  p->Outdent();
  p->Print("}\n");
}

void PrintImports(Printer* p) {
  p->Print(
      "import "
      "java.util.concurrent.CompletableFuture;\n");
  p->Print(
        "import "
        "java.util.concurrent.atomic.AtomicBoolean;\n");
}

void GenerateService(const ServiceDescriptor* service,
                     google::protobuf::io::ZeroCopyOutputStream* out,
                     ProtoFlavor flavor,
                     bool disable_version) {
  // All non-generated classes must be referred by fully qualified names to
  // avoid collision with generated classes.
  std::map<string, string> vars;
  vars["String"] = "java.lang.String";
  vars["Deprecated"] = "java.lang.Deprecated";
  vars["Override"] = "java.lang.Override";
  vars["Iterator"] = "java.util.Iterator";
  vars["Generated"] = "javax.annotation.Generated";
  vars["CompletableFuture"] =
      "java.util.concurrent.CompletableFuture";
  vars["AtomicBoolean"] =
        "java.util.concurrent.atomic.AtomicBoolean";
  vars["ProtobufUtils"] =
        "org.apache.dubbo.common.serialize.protobuf.support.ProtobufUtils";

  Printer printer(out, '$');
  string package_name = ServiceJavaPackage(service->file(),false);
  if (!package_name.empty()) {
    printer.Print(
        "package $package_name$;\n\n",
        "package_name", package_name);
  }

  PrintImports(&printer);

  // Package string is used to fully qualify method names.
  vars["Package"] = service->file()->package();
  if (!vars["Package"].empty()) {
    vars["Package"].append(".");
  }
  PrintService(service, &vars, &printer, false);
}

string ServiceJavaPackage(const FileDescriptor* file, bool nano) {
  string result = google::protobuf::compiler::java::ClassName(file);
  size_t last_dot_pos = result.find_last_of('.');
  if (last_dot_pos != string::npos) {
    result.resize(last_dot_pos);
  } else {
    result = "";
  }
  if (nano) {
    if (!result.empty()) {
      result += ".";
    }
    result += "nano";
  }
  return result;
}

string ServiceClassName(const google::protobuf::ServiceDescriptor* service) {
  return service->name() + "Dubbo";
}

}  // namespace java_dubbo_generator
