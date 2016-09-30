package gpbench

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

/**
 * Created by sudhir on 29/09/16.
 */
class DomainUtils {

	final static List<String> IGNORED_PROPERTIES = ["id", "version", "createdBy", "createdDate", "editedBy", "editedDate", "num"]


	public static def copyDomain(def instance, def old) {
		if(instance == null) throw new IllegalArgumentException("Copy is null")
		if(old == null) return null

		instance.domainClass.persistentProperties.each { GrailsDomainClassProperty dp ->
			if(IGNORED_PROPERTIES.contains(dp.name) || dp.identity) return
			if(dp.isAssociation()) return

			String name = dp.name
			instance[name] = old[name]
		}


		return instance
	}
}
