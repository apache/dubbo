use jni::sys::jlong;

#[link(wasm_import_module = "dubbo")]
extern "C" {
    fn hello_java(arg_id: i64) -> i64;
}

#[no_mangle]
pub unsafe extern "C" fn invoke(arg_id: jlong) {
    println!("hello from rust {arg_id}");
    let r = hello_java(arg_id);
    println!("hello_java {r}");
}
