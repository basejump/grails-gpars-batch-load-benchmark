package gpbench

class Country {

	Integer geoWorldMapId
	String code
	String fips104

	static mapping = {
		cache true
	}

	static constraints = {
		geoWorldMapId 	unique:true
		code			unique:true
		fips104			nullable:true
	}

	String toString() { code }

}
