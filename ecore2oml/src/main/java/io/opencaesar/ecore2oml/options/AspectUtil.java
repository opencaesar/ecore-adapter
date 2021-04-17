package io.opencaesar.ecore2oml.options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AspectUtil {
	
	private Map<String, Aspect> aspects = new HashMap<>();
	static private AspectUtil _instance = new AspectUtil();

	static public AspectUtil getInstance() {
		return _instance;
	}

	static public void init(List<Aspect> aspects) {
		synchronized (AspectUtil.class) {
			for (Aspect aspect : aspects) {
				_instance.addAspect(aspect.root, aspect);
			}
		}
	}

	private void addAspect(String root, Aspect aspect) {
		aspects.put(root, aspect);
	}


}
