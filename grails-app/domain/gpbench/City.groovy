package gpbench;

class City {

	Integer geoWorldMapId

	Region region

	String code
	String shortCode

	Float latitude
	Float longitude

	static belongsTo = Region

	static constraints = {
		code			blank:false
		//geoWorldMapId 	unique:true
	}

	String toString() {
		code
	}

}
