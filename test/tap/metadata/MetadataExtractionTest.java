package tap.metadata;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2014 - Astronomisches Rechen Institute (ARI)
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import tap.metadata.TAPTable.TableType;

/**
 * @author Gr&eacute;gory Mantelet (ARI) - gmantele@ari.uni-heidelberg.de
 * @version 1.1 (04/2014)
 */
public class MetadataExtractionTest {

	public static void main(String[] args) throws Throwable{
		MetadataExtractionTest extractor = new MetadataExtractionTest();
		try{
			extractor.connect();
			extractor.printTableMetadata("gums");
		}finally{
			extractor.close();
		}
	}

	private Connection connection = null;
	private Statement statement = null;

	public void connect(){
		try{
			Class.forName("org.postgresql.Driver");
			connection = DriverManager.getConnection("jdbc:postgresql:gmantele", "gmantele", "pwd");
			statement = connection.createStatement();
			System.out.println("[OK] DB connection successfully established !");
		}catch(ClassNotFoundException notFoundException){
			notFoundException.printStackTrace();
			System.err.println("[ERROR] Connection error !");
		}catch(SQLException sqlException){
			sqlException.printStackTrace();
			System.err.println("[ERROR] Connection error !");
		}
	}

	public ResultSet query(String requet){
		ResultSet resultat = null;
		try{
			resultat = statement.executeQuery(requet);
		}catch(SQLException e){
			e.printStackTrace();
			System.out.println("Erreur dans la requête: " + requet);
		}
		return resultat;

	}

	public TAPSchema printTableMetadata(final String table){
		try{

			DatabaseMetaData dbMeta = connection.getMetaData();
			TAPSchema tapSchema = null;
			TAPTable tapTable = null;

			// Extract Table metadata (schema, table, type):
			ResultSet rs = dbMeta.getTables(null, null, table, null);
			rs.last();
			if (rs.getRow() == 0)
				System.err.println("[ERROR] No found table for \"" + table + "\" !");
			else if (rs.getRow() > 1){
				rs.first();
				System.err.println("[ERROR] More than one match for \"" + table + "\":");
				while(rs.next())
					System.err.println(rs.getString(2) + "." + rs.getString(3) + " : " + rs.getString(4));
			}else{
				rs.first();
				tapSchema = new TAPSchema(rs.getString(2));
				TableType tableType = TableType.table;
				if (rs.getString(4) != null){
					try{
						tableType = TableType.valueOf(rs.getString(4));
					}catch(IllegalArgumentException iae){}
				}
				tapTable = new TAPTable(rs.getString(3), tableType);
				tapSchema.addTable(tapTable);
				System.out.println("[OK] 1 table FOUND ! => " + tapTable + " : " + tapTable.getType());
			}

			// Extract all columns metadata (type, precision, scale):
			rs = dbMeta.getColumns(null, tapSchema.getDBName(), tapTable.getDBName(), null);
			String type;
			while(rs.next()){
				type = rs.getString(6);
				if (type.endsWith("char") || type.equals("numeric")){
					type += "(" + rs.getInt(7);
					if (type.startsWith("numeric"))
						type += "," + rs.getInt(9);
					type += ")";
				}
				System.out.println("    * " + rs.getString(4) + " : " + type);
			}

			// Extract all indexed columns:
			rs = dbMeta.getIndexInfo(null, tapSchema.getDBName(), tapTable.getDBName(), false, true);
			while(rs.next()){
				System.out.println("    # " + rs.getString(6) + " : " + rs.getShort(7) + " (unique ? " + (!rs.getBoolean(4)) + ") -> " + rs.getString(9) + " => " + rs.getInt(11) + " unique values in the index ; " + rs.getInt(12) + " pages");
			}

			return tapSchema;

		}catch(SQLException e){
			e.printStackTrace();
			return null;
		}
	}

	public void close(){
		try{
			connection.close();
			statement.close();
			System.out.println("[OK] Connection closed !");
		}catch(SQLException e){
			e.printStackTrace();
			System.out.println("[ERROR] Connection CAN NOT be closed !");
		}
	}

}
