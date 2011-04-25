dataSource {
    pooled = true
    driverClassName = "org.hsqldb.jdbcDriver"
    username = "sa"
    password = ""
}
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
	 jdbc.batch_size = 50
}
// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = "create" // one of 'create', 'create-drop','update'
            //url = "jdbc:hsqldb:mem:devDb;shutdown=true"

				dialect = 'org.hibernate.dialect.MySQL5InnoDBDialect'
				driverClassName = 'com.mysql.jdbc.Driver'
				username = 'root'
				password = System.properties['db.password']
				pooled = true
				url = 'jdbc:mysql://127.0.0.1/gpbench'
				//loggingSql = true
        }
    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:hsqldb:mem:testDb"
        }
    }
    production {
        dataSource {
            dbCreate = "update"
            //url = "jdbc:hsqldb:file:prodDb;shutdown=true"
				dialect = 'org.hibernate.dialect.MySQL5InnoDBDialect'
				driverClassName = 'com.mysql.jdbc.Driver'
				username = 'root'
				password = System.properties['db.password']
				pooled = true
				url = 'jdbc:mysql://127.0.0.1/gpbench'
        }
    }
}
