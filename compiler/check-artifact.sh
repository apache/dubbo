#!/bin/bash

# Check that the codegen artifacts are of correct architecture and don't have
# unexpected dependencies.
# To be run from Gradle.
# Usage: check-artifact <OS> <ARCH>
# <OS> and <ARCH> are ${os.detected.name} and ${os.detected.arch} from
# osdetector-gradle-plugin
OS=$1
ARCH=$2

if [[ $# < 2 ]]; then
  echo "No arguments provided. This script is intended to be run from Gradle."
  exit 1
fi

# Under Cygwin, bash doesn't have these in PATH when called from Gradle which
# runs in Windows version of Java.
export PATH="/bin:/usr/bin:$PATH"

E_PARAM_ERR=98
E_ASSERT_FAILED=99

# Usage: fail ERROR_MSG
fail()
{
  echo "ERROR: $1"
  echo
  exit $E_ASSERT_FAILED
}

# Usage: assertEq VAL1 VAL2 $LINENO
assertEq ()
{
  lineno=$3
  if [ -z "$lineno" ]; then
    echo "lineno not given"
    exit $E_PARAM_ERR
  fi

  if [[ "$1" != "$2" ]]; then
    echo "Assertion failed:  \"$1\" == \"$2\""
    echo "File \"$0\", line $lineno"    # Give name of file and line number.
    exit $E_ASSERT_FAILED
  fi
}

# Checks the artifact is for the expected architecture
# Usage: checkArch <path-to-protoc>
checkArch ()
{
  echo
  echo "Checking format of $1"
  if [[ "$OS" == windows || "$OS" == linux ]]; then
    format="$(objdump -f "$1" | grep -o "file format .*$" | grep -o "[^ ]*$")"
    echo Format=$format
    if [[ "$OS" == linux ]]; then
      if [[ "$ARCH" == x86_32 ]]; then
        assertEq "$format" "elf32-i386" $LINENO
      elif [[ "$ARCH" == x86_64 ]]; then
        assertEq "$format" "elf64-x86-64" $LINENO
      else
        fail "Unsupported arch: $ARCH"
      fi
    else
      # $OS == windows
      if [[ "$ARCH" == x86_32 ]]; then
        assertEq "$format" "pei-i386" $LINENO
      elif [[ "$ARCH" == x86_64 ]]; then
        assertEq "$format" "pei-x86-64" $LINENO
      else
        fail "Unsupported arch: $ARCH"
      fi
    fi
  elif [[ "$OS" == osx ]]; then
    format="$(file -b "$1" | grep -o "[^ ]*$")"
    echo Format=$format
    if [[ "$ARCH" == x86_32 ]]; then
      assertEq "$format" "i386" $LINENO
    elif [[ "$ARCH" == x86_64 ]]; then
      assertEq "$format" "x86_64" $LINENO
    else
      fail "Unsupported arch: $ARCH"
    fi
  else
    fail "Unsupported system: $OS"
  fi
  echo
}

# Checks the dependencies of the artifact. Artifacts should only depend on
# system libraries.
# Usage: checkDependencies <path-to-protoc>
checkDependencies ()
{
  echo "Checking dependencies of $1"
  if [[ "$OS" == windows ]]; then
    dump_cmd='objdump -x '"$1"' | fgrep "DLL Name"'
    white_list="KERNEL32\.dll\|msvcrt\.dll\|USER32\.dll"
  elif [[ "$OS" == linux ]]; then
    dump_cmd='ldd '"$1"
    if [[ "$ARCH" == x86_32 ]]; then
      white_list="linux-gate\.so\.1\|libpthread\.so\.0\|libm\.so\.6\|libc\.so\.6\|ld-linux\.so\.2"
    elif [[ "$ARCH" == x86_64 ]]; then
      white_list="linux-vdso\.so\.1\|libpthread\.so\.0\|libm\.so\.6\|libc\.so\.6\|ld-linux-x86-64\.so\.2"
    fi
  elif [[ "$OS" == osx ]]; then
    set +x
    dump_cmd='otool -L '"$1"' | fgrep dylib'
    white_list="libz\.1\.dylib\|libc++.1.dylib\|libstdc++\.6\.dylib\|libSystem\.B\.dylib"
    set -x
  fi
  if [[ -z "$white_list" || -z "$dump_cmd" ]]; then
    fail "Unsupported platform $OS-$ARCH."
  fi
  echo "Checking for expected dependencies ..."
  eval $dump_cmd | grep -i "$white_list" || fail "doesn't show any expected dependencies"
  echo "Checking for unexpected dependencies ..."
  eval $dump_cmd | grep -i -v "$white_list"
  ret=$?
  if [[ $ret == 0 ]]; then
    fail "found unexpected dependencies (listed above)."
  elif [[ $ret != 1 ]]; then
    fail "Error when checking dependencies."
  fi  # grep returns 1 when "not found", which is what we expect
  echo "Dependencies look good."
  echo
}

FILE="build/artifacts/java_plugin/protoc-gen-dubbo-java.exe"
checkArch "$FILE" && checkDependencies "$FILE"
