package gpbench

import grails.compiler.GrailsCompileStatic
import grails.transaction.Transactional

@Transactional
class CityService {

	@GrailsCompileStatic
	public void insertWithDataBinding(Map row) {
		if(City.exists((row.id as Long))) return
		City c = new City(row)
		c.id = row.id as Long
		c.save(failOnError: true)
	}

	@GrailsCompileStatic
	public void insertWithSetter(Map row) {
		if(City.exists((row.id as Long))) return
		Region r = Region.load(row['region']['id'] as Long)
		Country country = Country.load(row['country']['id'] as Long)

		City c = new City()
		c.id = row.id as Long
		DomainUtils.copyDomain(c, row)
		c.region = r
		c.country = country
		c.save(failOnError: true)
	}

}
