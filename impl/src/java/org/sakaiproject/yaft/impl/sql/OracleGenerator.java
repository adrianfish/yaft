package org.sakaiproject.yaft.impl.sql;

public class OracleGenerator extends DefaultSqlGenerator
{
	public OracleGenerator()
	{
		VARCHAR   = "VARCHAR2";
		TIMESTAMP = "TIMESTAMP";
		TEXT      = "LONG";
		BOOL = "char(1)";
		MEDIUMTEXT = "LONG";
	}
}
