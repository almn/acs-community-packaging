package org.alfresco.repo.web.scripts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.TemplateService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.web.scripts.AbstractRuntimeContainer;
import org.alfresco.web.scripts.Authenticator;
import org.alfresco.web.scripts.Description;
import org.alfresco.web.scripts.ServerModel;
import org.alfresco.web.scripts.WebScript;
import org.alfresco.web.scripts.WebScriptException;
import org.alfresco.web.scripts.WebScriptRequest;
import org.alfresco.web.scripts.WebScriptResponse;
import org.alfresco.web.scripts.Description.RequiredAuthentication;
import org.alfresco.web.scripts.Description.RequiredTransaction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class RepositoryContainer extends AbstractRuntimeContainer
{
    // Logger
    protected static final Log logger = LogFactory.getLog(RepositoryContainer.class);

    /** Component Dependencies */
    private Repository repository;
    private RepositoryImageResolver imageResolver;
    private RetryingTransactionHelper retryingTransactionHelper;
    private AuthorityService authorityService;
    private PermissionService permissionService;
    private DescriptorService descriptorService;

    
    public void setRepository(Repository repository)
    {
        this.repository = repository;
    }

    public void setRepositoryImageResolver(RepositoryImageResolver imageResolver)
    {
        this.imageResolver = imageResolver;
    }
    
    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

    public void setTransactionHelper(RetryingTransactionHelper retryingTransactionHelper)
    {
        this.retryingTransactionHelper = retryingTransactionHelper;
    }

    public void setDescriptorService(DescriptorService descriptorService)
    {
        this.descriptorService = descriptorService;
    }

    public void setAuthorityService(AuthorityService authorityService)
    {
        this.authorityService = authorityService;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.Container#getDescription()
     */
    public ServerModel getDescription()
    {
        return new RepositoryServerModel(descriptorService.getServerDescriptor());
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.AbstractRuntimeContainer#getScriptParameters()
     */
    public Map<String, Object> getScriptParameters()
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(super.getScriptParameters());
        addRepoParameters(params);
        return params;
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.AbstractRuntimeContainer#getTemplateParameters()
     */
    public Map<String, Object> getTemplateParameters()
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(super.getTemplateParameters());
        params.put(TemplateService.KEY_IMAGE_RESOLVER, imageResolver.getImageResolver());
        addRepoParameters(params);
        return params;
    }

    /**
     * Add Repository specific parameters
     * 
     * @param params
     */
    private void addRepoParameters(Map<String, Object> params)
    {
        if (AlfrescoTransactionSupport.getTransactionId() != null)
        {
            NodeRef rootHome = repository.getRootHome();
            if (rootHome != null && permissionService.hasPermission(rootHome, PermissionService.READ).equals(AccessStatus.ALLOWED))
            {
                params.put("roothome", rootHome);
            }
            NodeRef companyHome = repository.getCompanyHome();
            if (companyHome != null && permissionService.hasPermission(companyHome, PermissionService.READ).equals(AccessStatus.ALLOWED))
            {
                params.put("companyhome", companyHome);
            }
            NodeRef person = repository.getPerson();
            if (person != null && permissionService.hasPermission(companyHome, PermissionService.READ).equals(AccessStatus.ALLOWED))
            {
                params.put("person", person);
                params.put("userhome", repository.getUserHome(person));
            }
        }
    }

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.RuntimeContainer#executeScript(org.alfresco.web.scripts.WebScriptRequest, org.alfresco.web.scripts.WebScriptResponse, org.alfresco.web.scripts.Authenticator)
     */
    public void executeScript(WebScriptRequest scriptReq, WebScriptResponse scriptRes, Authenticator auth)
        throws IOException
    {
        WebScript script = scriptReq.getServiceMatch().getWebScript();
        Description desc = script.getDescription();
        RequiredAuthentication required = desc.getRequiredAuthentication();
        boolean isGuest = scriptReq.isGuest();
        
        if (required == RequiredAuthentication.none)
        {
            AuthenticationUtil.clearCurrentSecurityContext();
            transactionedExecute(script, scriptReq, scriptRes);
        }
        else if ((required == RequiredAuthentication.user || required == RequiredAuthentication.admin) && isGuest)
        {
            throw new WebScriptException(HttpServletResponse.SC_UNAUTHORIZED, "Web Script " + desc.getId() + " requires user authentication; however, a guest has attempted access.");
        }
        else
        {
            String currentUser = null;

            try
            {
                //
                // Determine if user already authenticated
                //
                currentUser = AuthenticationUtil.getCurrentUserName();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Current authentication: " + (currentUser == null ? "unauthenticated" : "authenticated as " + currentUser));
                    logger.debug("Authentication required: " + required);
                    logger.debug("Guest login: " + isGuest);
                }

                //
                // Apply appropriate authentication to Web Script invocation
                //
                if (auth == null || auth.authenticate(required, isGuest))
                {
                    if (required == RequiredAuthentication.admin && !authorityService.hasAdminAuthority())
                    {
                        throw new WebScriptException(HttpServletResponse.SC_UNAUTHORIZED, "Web Script " + desc.getId() + " requires admin authentication; however, a non-admin has attempted access.");
                    }
                    
                    // Execute Web Script
                    transactionedExecute(script, scriptReq, scriptRes);
                }
            }
            finally
            {
                //
                // Reset authentication for current thread
                //
                AuthenticationUtil.clearCurrentSecurityContext();
                if (currentUser != null)
                {
                    AuthenticationUtil.setCurrentUser(currentUser);
                }
                
                if (logger.isDebugEnabled())
                    logger.debug("Authentication reset: " + (currentUser == null ? "unauthenticated" : "authenticated as " + currentUser));                
            }
        }
    }

    /**
     * Execute script within required level of transaction
     * 
     * @param scriptReq
     * @param scriptRes
     * @throws IOException
     */
    protected void transactionedExecute(final WebScript script, final WebScriptRequest scriptReq, final WebScriptResponse scriptRes)
        throws IOException
    {
        final Description description = script.getDescription();
        if (description.getRequiredTransaction() == RequiredTransaction.none)
        {
            script.execute(scriptReq, scriptRes);
        }
        else
        {
            // encapsulate script within transaction
            RetryingTransactionCallback<Object> work = new RetryingTransactionCallback<Object>()
            {
                public Object execute() throws Exception
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Begin transaction: " + description.getRequiredTransaction());
                    
                    script.execute(scriptReq, scriptRes);
                    
                    if (logger.isDebugEnabled())
                        logger.debug("End transaction: " + description.getRequiredTransaction());
                    
                    return null;
                }        
            };
        
            if (description.getRequiredTransaction() == RequiredTransaction.required)
            {
                retryingTransactionHelper.doInTransaction(work); 
            }
            else
            {
                retryingTransactionHelper.doInTransaction(work, false, true); 
            }
        }
    }

}