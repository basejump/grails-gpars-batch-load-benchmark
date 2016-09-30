package gpbench;

class City {

	String name
	String shortCode

	Float latitude
	Float longitude

	static belongsTo = [region:Region, country:Country]

	static mapping = {
		id generator: "assigned"
		cache true
	}

	static constraints = {
		name blank: false
	}

	String toString() { name }

}
