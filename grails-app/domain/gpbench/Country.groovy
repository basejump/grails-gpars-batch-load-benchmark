package gpbench

class Country {

	Integer geoWorldMapId

	String code
	String fips104

	static mapping = {
		cache true
		id generator: 'assigned'
	}

	static constraints = {
		geoWorldMapId 	unique:true
		code			unique:true
		fips104			nullable:true
	}

	String toString() { code }

}
