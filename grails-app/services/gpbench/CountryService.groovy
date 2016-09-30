package gpbench

import grails.compiler.GrailsCompileStatic
import grails.transaction.Transactional
import org.springframework.jdbc.core.JdbcTemplate

@Transactional
class CountryService {

	@GrailsCompileStatic
	public void insertWithDataBinding(Map row) {
		Country c = new Country(row)
		c.id = row.id as Long
		c.save(failOnError: true)
	}

	@GrailsCompileStatic
	public void insertWithSetter(Map row) {
		Country c = new Country()
		c.id = row.id as Long
		DomainUtils.copyDomain(c, row)
		c.save(failOnError: true)
	}


}
