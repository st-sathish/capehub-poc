package com.capestartproject.userdirectory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import com.capestartproject.common.security.api.Role;
import com.capestartproject.common.util.NotFoundException;
import com.capestartproject.kernel.security.persistence.JpaOrganization;

/**
 * Utility class for user directory persistence methods
 */
public final class UserDirectoryPersistenceUtil {

  private UserDirectoryPersistenceUtil() {
  }

  /**
   * Persist a set of roles
   *
   * @param roles
   *          the roles to persist
   * @param emf
   *          the entity manager factory
   * @return the persisted roles
   */
  public static Set<JpaRole> saveRoles(Set<? extends Role> roles, EntityManagerFactory emf) {
    Set<JpaRole> updatedRoles = new HashSet<JpaRole>();
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      // Save or udpate roles
      for (Role role : roles) {
        JpaRole jpaRole = (JpaRole) role;
        saveOrganization((JpaOrganization) jpaRole.getOrganization(), emf);
        JpaRole findRole = findRole(jpaRole.getName(), jpaRole.getOrganization().getId(), emf);
        if (findRole == null) {
          em.persist(jpaRole);
          updatedRoles.add(jpaRole);
        } else {
          findRole.setDescription(jpaRole.getDescription());
          updatedRoles.add(em.merge(findRole));
        }
      }
      tx.commit();
      return updatedRoles;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * Persist an organization
   *
   * @param organization
   *          the organization to persist
   * @param emf
   *          the entity manager factory
   * @return the persisted organization
   */
  public static JpaOrganization saveOrganization(JpaOrganization organization, EntityManagerFactory emf) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaOrganization org = findOrganization(organization, emf);
      if (org == null) {
        em.persist(organization);
      } else {
        organization = em.merge(org);
      }
      tx.commit();
      return organization;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * Persist an user
   * 
   * @param user
   *          the user to persist
   * @param emf
   *          the entity manager factory
   * @return the persisted organization
   */
  public static JpaUser saveUser(JpaUser user, EntityManagerFactory emf) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaUser u = findUser(user.getUsername(), user.getOrganization().getId(), emf);
      if (u == null) {
        em.persist(user);
      } else {
        u.password = user.getPassword();
        u.roles = user.roles;
        user = em.merge(u);
      }
      tx.commit();
      return user;
    } finally {
      if (tx.isActive()) {
        tx.rollback();
      }
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns all groups from the persistence unit as a list
   *
   * @param organization
   *          the organization
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the group list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaGroup> findGroups(String organization, int limit, int offset, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Group.findAll").setMaxResults(limit).setFirstResult(offset);
      query.setParameter("organization", organization);
      return query.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns all roles from the persistence unit as a list
   *
   * @param organization
   *          the organization
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the roles list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaRole> findRoles(String organization, int limit, int offset, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Role.findAll").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("org", organization);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns a list of roles by a search query if set or all roles if search query is <code>null</code>
   *
   * @param orgId
   *          the organization identifier
   * @param query
   *          the query to search
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the roles list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaRole> findRolesByQuery(String orgId, String query, int limit, int offset,
          EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Role.findByQuery").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("query", query.toUpperCase());
      q.setParameter("org", orgId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns all user groups from the persistence unit as a list
   *
   * @param userName
   *          the user name
   * @param emf
   *          the entity manager factory
   * @return the group list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaGroup> findGroupsByUser(String userName, String orgId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Group.findByUser");
      query.setParameter("username", userName);
      query.setParameter("organization", orgId);
      return query.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted organization by the given organization
   *
   * @param organization
   *          the organization
   * @param emf
   *          the entity manager factory
   * @return the organization or <code>null</code> if not found
   */
  public static JpaOrganization findOrganization(JpaOrganization organization, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Organization.findById");
      query.setParameter("id", organization.getId());
      return (JpaOrganization) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted user by the user name and organization id
   *
   * @param userName
   *          the user name
   * @param organizationId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the user or <code>null</code> if not found
   */
  public static JpaUser findUser(String userName, String organizationId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.findByUsername");
      q.setParameter("u", userName);
      q.setParameter("org", organizationId);
      return (JpaUser) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns a list of users by a search query if set or all users if search query is <code>null</code>
   *
   * @param orgId
   *          the organization identifier
   * @param query
   *          the query to search
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the users list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaUser> findUsersByQuery(String orgId, String query, int limit, int offset,
          EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.findByQuery").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("query", query.toUpperCase());
      q.setParameter("org", orgId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns a list of users by a search query if set or all users if search query is <code>null</code>
   *
   * @param query
   *          the query to search
   * @param limit
   *          the limit
   * @param offset
   *          the offset
   * @param emf
   *          the entity manager factory
   * @return the users list
   */
  @SuppressWarnings("unchecked")
  public static List<JpaUser> findUsers(String orgId, int limit, int offset, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("User.findAll").setMaxResults(limit).setFirstResult(offset);
      q.setParameter("org", orgId);
      return q.getResultList();
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted role by the name and organization id
   *
   * @param name
   *          the role name
   * @param organization
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the user or <code>null</code> if not found
   */
  public static JpaRole findRole(String name, String organization, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query query = em.createNamedQuery("Role.findByName");
      query.setParameter("name", name);
      query.setParameter("org", organization);
      return (JpaRole) query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Returns the persisted group by the group id and organization id
   *
   * @param groupId
   *          the group id
   * @param orgId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @return the group or <code>null</code> if not found
   */
  public static JpaGroup findGroup(String groupId, String orgId, EntityManagerFactory emf) {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      Query q = em.createNamedQuery("Group.findById");
      q.setParameter("groupId", groupId);
      q.setParameter("organization", orgId);
      return (JpaGroup) q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  public static void removeGroup(String groupId, String orgId, EntityManagerFactory emf) throws NotFoundException,
          Exception {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaGroup group = findGroup(groupId, orgId, emf);
      if (group == null) {
        throw new NotFoundException("Group with ID " + groupId + " does not exist");
      }
      em.remove(em.merge(group));
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      em.close();
    }
  }

  /**
   * Delete the user with given name in the given organization
   * 
   * @param username
   *          the name of the user to delete
   * @param orgId
   *          the organization id
   * @param emf
   *          the entity manager factory
   * @throws NotFoundException
   * @throws Exception
   */
  public static void deleteUser(String username, String orgId, EntityManagerFactory emf) throws NotFoundException,
          Exception {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      JpaUser user = findUser(username, orgId, emf);
      if (user == null) {
        throw new NotFoundException("User with name " + username + " does not exist");
      }
      em.remove(em.merge(user));
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    } finally {
      em.close();
    }
  }

}
