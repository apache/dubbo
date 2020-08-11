function route(invokers,invocation,context){ 
	var result = new java.util.ArrayList(invokers.size());
	
	for (i=0;i<invokers.size(); i++){ 
	    if (invokers.get(i).isAvailable()) {
	        result.add(invokers.get(i)) ;
	    }
	} ; 
	return result;
};
route(invokers,invocation,context);