/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.distribution.serialization.impl.vlt;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.ObservationManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.apache.jackrabbit.vault.fs.api.IdConflictPolicy;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.ExportOptions;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.serialization.DistributionExportFilter;
import org.apache.sling.distribution.serialization.DistributionExportOptions;
import org.apache.sling.distribution.serialization.ImportSettings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link FileVaultContentSerializer}
 */
public class FileVaultContentSerializerTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Before
    public void setUp() throws Exception {
        // create test content
        MockHelper helper = MockHelper.create(context.resourceResolver()).resource("/libs").p("prop", "value")
                .resource("sub").p("sub", "hello")
                .resource(".sameLevel")
                .resource("/apps").p("foo", "baa");
        helper.commit();
        
        // register sling node types
        Session session = context.resourceResolver().adaptTo(Session.class);
        RepositoryUtil.registerSlingNodeTypes(session);
    }

    @Test
    public void testExportToStream() throws Exception {
        Packaging packaging = mock(Packaging.class);
        ImportSettings importSettings = new ImportSettings(ImportMode.REPLACE, AccessControlHandling.IGNORE,
                AccessControlHandling.IGNORE, 1024, false, false, IdConflictPolicy.LEGACY);

        String[] packageRoots = new String[]{"/etc/packages"};
        String[] nodeFilters = new String[0];
        String[] propertyFilters = new String[0];
        boolean useReferences = false;
        int threshold = 1024;
        FileVaultContentSerializer fileVaultContentSerializer = new FileVaultContentSerializer("vlt", packaging, packageRoots, nodeFilters,
                propertyFilters, useReferences, new HashMap<String, String>(), importSettings);

        ResourceResolver sessionResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);

        PackageManager pm = mock(PackageManager.class);
        when(packaging.getPackageManager()).thenReturn(pm);
        OutputStream outputStream = new ByteArrayOutputStream();

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                return null;
            }
        }).when(pm).assemble(same(session), any(ExportOptions.class), same(outputStream));

        Workspace workspace = mock(Workspace.class);
        ObservationManager observationManager = mock(ObservationManager.class);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        when(session.getWorkspace()).thenReturn(workspace);
        when(sessionResolver.adaptTo(Session.class)).thenReturn(session);
        DistributionExportFilter filter = mock(DistributionExportFilter.class);
        DistributionRequest request = mock(DistributionRequest.class);
        when(request.getPaths()).thenReturn(new String[]{"/libs"});
        when(request.getFilters("/libs")).thenReturn(new String[0]);
        DistributionExportOptions exportOptions = new DistributionExportOptions(request, filter);

        fileVaultContentSerializer.exportToStream(sessionResolver, exportOptions, outputStream);
    }

    @Test
    public void testImportFromStream() throws Exception {
        Packaging packaging = mock(Packaging.class);
        ImportSettings importSettings = new ImportSettings(ImportMode.REPLACE, AccessControlHandling.IGNORE,
                AccessControlHandling.IGNORE, 1024, true, true, IdConflictPolicy.LEGACY);

        String[] packageRoots = new String[]{"/"};
        String[] nodeFilters = new String[0];
        String[] propertyFilters = new String[0];
        boolean useReferences = false;
        FileVaultContentSerializer fileVaultContentSerializer = new FileVaultContentSerializer("vlt", packaging, packageRoots, nodeFilters,
                propertyFilters, useReferences, new HashMap<String, String>(), importSettings);

        File file = new File(getClass().getResource("/vlt/dp.vlt").getFile());

        fileVaultContentSerializer.importFromStream(context.resourceResolver(), new FileInputStream(file));
    }
}