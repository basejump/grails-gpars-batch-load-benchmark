// Place your Spring DSL code here
beans = {
	jdbcTemplate(org.springframework.jdbc.core.JdbcTemplate, ref("dataSource"))
}

