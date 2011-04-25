package gpbench;

class Region {

	Integer geoWorldMapId;

	Country country

	String code
	String shortCode

	static belongsTo = Country

	static mapping = {
		cache true
		geoWorldMapId unique:true, index:'geoWorldMapId_Idx'
		
	}
	
	static constraints = { 
		code nullable:false 
	}

	String toString() {
		code
	}

}
