package com.othersonline.kv.distributed.impl;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.othersonline.kv.distributed.AbstractRefreshingNodeStore;
import com.othersonline.kv.distributed.ConfigurationException;
import com.othersonline.kv.distributed.Node;
import com.othersonline.kv.distributed.NodeStore;

/**
 * Reads nodes from a jdbc database. Table should look like this:
 * 
 * create table node ( id int primary key, physical_id int not null, salt
 * varchar(10) unique not null, connection_uri varchar(50) not null, status
 * enum('1', '2', '3'));
 * 
 * Nodes with a status other than 1 will be ignored.
 * 
 * @author sam
 * 
 */
public class JdbcNodeStore extends AbstractRefreshingNodeStore implements
		NodeStore {
	public static final String DATA_SOURCE_PROPERTY = "nodeStore.dataSource";

	private String dataSourceName;

	private DataSource ds;

	public JdbcNodeStore() {
		super();
	}

	public JdbcNodeStore(String dataSourceName) {
		super();
		this.dataSourceName = dataSourceName;
	}

	public void setDataSourceJndiName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}

	@Override
	public void addNode(Node node) {
		Connection conn = null;
		PreparedStatement select = null;
		PreparedStatement upsert = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			// would love to use ON DUPLICATE KEY UPDATE here but for
			// compatibility with non-mysql databases I'm not going to do so.
			select = conn
					.prepareStatement("select count(*) as count from node where id = ?");
			select.setInt(1, node.getId());
			rs = select.executeQuery();
			boolean update = false;
			if (rs.next()) {
				int count = rs.getInt(1);
				update = (count > 0);
			}
			if (update) {
				upsert = conn
						.prepareStatement("update node set status = ? where id = ?");
				upsert.setInt(1, 1);
				upsert.setInt(2, node.getId());
			} else {
				upsert = conn
						.prepareStatement("insert into node (id, physical_id, salt, connection_uri, status) values (?, ?, ?, ?, ?)");
				upsert.setInt(1, node.getId());
				upsert.setInt(2, node.getPhysicalId());
				upsert.setString(3, node.getSalt());
				upsert.setString(4, node.getConnectionURI());
				upsert.setInt(5, 1);
			}
			upsert.executeUpdate();
			if (!conn.getAutoCommit())
				conn.commit();

			// only add node if above code succeeded
			super.addNode(node);
		} catch (SQLException e) {
			log.error("SQLException adding node()", e);
		} catch (NamingException e) {
			log.error("NamingException adding node()", e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}
			if (select != null) {
				try {
					select.close();
				} catch (Exception e) {
				}
			}
			if (upsert != null) {
				try {
					upsert.close();
				} catch (Exception e) {
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
				}
			}
		}
	}

	@Override
	public void removeNode(Node node) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			// remove node before any sql operations
			super.removeNode(node);

			conn = getConnection();
			ps = conn
					.prepareStatement("update node set status = ? where id = ?");
			ps.setInt(1, 2);
			ps.setInt(2, node.getId());
			ps.execute();

			if (!conn.getAutoCommit())
				conn.commit();
		} catch (SQLException e) {
			log.error("SQLException adding node()", e);
		} catch (NamingException e) {
			log.error("NamingException adding node()", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
				}
			}
		}
	}

	@Override
	public List<Node> refreshActiveNodes() throws IOException,
			ConfigurationException {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			List<Node> nodes = new LinkedList<Node>();

			conn = getConnection();
			ps = conn
					.prepareStatement("select id, physical_id, salt, connection_uri from node where status = 1 order by id asc");
			rs = ps.executeQuery();
			while (rs.next()) {
				DefaultNodeImpl node = new DefaultNodeImpl();
				node.setConnectionURI(rs.getString("connection_uri"));
				node.setId(rs.getInt("id"));
				node.setPhysicalId(rs.getInt("physical_id"));
				node.setSalt(rs.getString("salt"));
				nodes.add(node);
			}
			return nodes;
		} catch (SQLException e) {
			throw new IOException(e);
		} catch (NamingException e) {
			throw new ConfigurationException(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
				}
			}
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {
				}
			}
		}
	}

	private Connection getConnection() throws SQLException, NamingException {
		if (ds == null) {
			Context initCtx = new InitialContext();
			ds = (DataSource) initCtx.lookup(dataSourceName);
		}
		return ds.getConnection();
	}
}