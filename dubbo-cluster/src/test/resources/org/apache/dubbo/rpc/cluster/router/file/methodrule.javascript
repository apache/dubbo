function route(invokers,invocation,context){ 
	var result = new java.util.ArrayList();
	if (invokers.size()>1 && invocation.getMethodName() .equals("method1")) {
	   	result.add(invokers.get(0)) ;
	} else {
		result.add(invokers.get(1)) ;
	}
	return result;
};
route(invokers,invocation,context);