# How to build the wasm file

1. install rustup

2. install rust

3. generate the wasm file

```shell
cd {dubbo}/dubbo-wasm/dubbo-wasm-rpc-api/src/test/rust-filter
cargo build --target wasm32-wasi --release
```

then you will see the wasm file
in `{dubbo}/dubbo-wasm/dubbo-wasm-rpc-api/src/test/rust-filter/target/wasm32-wasi/release/rust_filter.wasm`

4. rename the wasm file

rename the file to `org.apache.dubbo.wasm.rpc.AbstractWasmFilterTest$RustFilter.wasm`
