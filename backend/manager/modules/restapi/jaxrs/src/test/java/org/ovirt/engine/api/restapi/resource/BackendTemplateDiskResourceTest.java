package org.ovirt.engine.api.restapi.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.ovirt.engine.api.model.Action;
import org.ovirt.engine.api.model.Disk;
import org.ovirt.engine.api.model.StorageDomain;
import org.ovirt.engine.core.common.action.ActionParametersBase;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.ExportRepoImageParameters;
import org.ovirt.engine.core.common.action.MoveOrCopyImageGroupParameters;
import org.ovirt.engine.core.common.action.RemoveDiskParameters;
import org.ovirt.engine.core.common.businessentities.StorageDomainStatus;
import org.ovirt.engine.core.common.businessentities.StorageDomainType;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskStorageType;
import org.ovirt.engine.core.common.businessentities.storage.ImageOperation;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.businessentities.storage.PropagateErrors;
import org.ovirt.engine.core.common.businessentities.storage.StorageType;
import org.ovirt.engine.core.common.businessentities.storage.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.NameQueryParameters;
import org.ovirt.engine.core.common.queries.QueryParametersBase;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendTemplateDiskResourceTest
        extends AbstractBackendSubResourceTest<Disk, org.ovirt.engine.core.common.businessentities.storage.Disk, BackendTemplateDiskResource> {

    private static final Guid TEMPLATE_ID = GUIDS[0];
    private static final Guid DISK_ID = GUIDS[1];

    public BackendTemplateDiskResourceTest() {
        super(new BackendTemplateDiskResource(DISK_ID.toString(), TEMPLATE_ID));
    }

    @Test
    public void testGetNotFound() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(QueryType.GetVmTemplatesDisks,
                                     IdQueryParameters.class,
                                     new String[] { "Id" },
                                     new Object[] { TEMPLATE_ID },
                                     new ArrayList<DiskImage>());
        try {
            resource.get();
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testGet() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        setUpEntityQueryExpectations(1);

        Disk disk = resource.get();
        verifyModelSpecific(disk, 1);
        verifyLinks(disk);
    }

    @Override
    protected org.ovirt.engine.core.common.businessentities.storage.Disk getEntity(int index) {
        return setUpEntityExpectations(mock(DiskImage.class), index);
    }

    protected List<org.ovirt.engine.core.common.businessentities.storage.Disk> getEntityList() {
        List<org.ovirt.engine.core.common.businessentities.storage.Disk> entities = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            entities.add(getEntity(i));
        }
        return entities;

    }

    protected void setUpEntityQueryExpectations(int times) throws Exception {
        while (times-- > 0) {
            setUpEntityQueryExpectations(QueryType.GetVmTemplatesDisks,
                    IdQueryParameters.class,
                    new String[] { "Id" },
                    new Object[] { TEMPLATE_ID },
                    getEntityList());
        }
    }

    @Test
    public void testExport() throws Exception {
        setUriInfo(setUpActionExpectations(ActionType.ExportRepoImage,
                ExportRepoImageParameters.class,
                new String[]{"ImageGroupID", "DestinationDomainId"},
                new Object[]{GUIDS[1], GUIDS[3]}, true, true, null, null, true));

        Action action = new Action();
        action.setStorageDomain(new StorageDomain());
        action.getStorageDomain().setId(GUIDS[3].toString());

        verifyActionResponse(resource.export(action));
    }

    @Test
    public void testBadGuid() throws Exception {
        try {
            new BackendStorageDomainVmResource(null, "foo");
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyNotFoundException(wae);
        }
    }

    @Test
    public void testIncompleteExport() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        try {
            resource.export(new Action());
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
            verifyIncompleteException(wae, "Action", "export", "storageDomain.id|name");
        }
    }

    @Test
    public void testCopyBySdId() throws Exception {
        setUpEntityQueryExpectations(QueryType.GetDiskByDiskId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[1] },
                getEntity(3));
        setUriInfo(setUpActionExpectations(ActionType.MoveOrCopyDisk,
                                           MoveOrCopyImageGroupParameters.class,
                                           new String[] { "ImageId", "ImageGroupID", "SourceDomainId",
                                                   "StorageDomainId", "Operation" },
                                           new Object[] { GUIDS[1], GUIDS[3], Guid.Empty, GUIDS[3], ImageOperation
                                                   .Copy }));

        verifyActionResponse(resource.copy(setUpCopyParams(false)));
    }

    @Test
    public void testCopyBySdNameWithoutFilter() throws Exception {
        testCopyBySdName(false);
    }

    @Test
    public void testCopyBySdNameWithFilter() throws Exception {
        testCopyBySdName(true);
    }

    @Test
    public void testRemove() throws Exception {
        setUpGetEntityExpectations(1);
        setUriInfo(setUpActionExpectations(ActionType.RemoveDisk,
                RemoveDiskParameters.class,
                new String[] { "DiskId" },
                new Object[] { GUIDS[1] },
                true,
                true));
        verifyRemove(resource.remove());
    }

    @Test
    public void testRemoveByStorageDomain() throws Exception {
        setUpGetEntityExpectations(1);
        UriInfo uriInfo = setUpActionExpectations(
            ActionType.RemoveDisk,
            RemoveDiskParameters.class,
            new String[] { "DiskId" },
            new Object[] { GUIDS[1] },
            true,
            true,
            false
        );
        uriInfo = addMatrixParameterExpectations(uriInfo, BackendTemplateDiskResource.STORAGE_DOMAIN, GUIDS[0].toString());
        setUriInfo(uriInfo);
        verifyRemove(resource.remove());
    }

    @Test
    public void testRemoveForced() throws Exception {
        setUpGetEntityExpectations(1);
        UriInfo uriInfo = setUpActionExpectations(
            ActionType.RemoveDisk,
            RemoveDiskParameters.class,
            new String[] { "DiskId" },
            new Object[] { GUIDS[1] },
            true,
            true,
            false
        );
        uriInfo = addMatrixParameterExpectations(uriInfo, BackendTemplateDiskResource.FORCE, Boolean.TRUE.toString());
        setUriInfo(uriInfo);
        verifyRemove(resource.remove());
    }

    private void setUpGetEntityExpectations(int times) {
        for (int i = 0; i < times; i++) {
            setUpEntityQueryExpectations(QueryType.GetVmTemplatesDisks,
                    IdQueryParameters.class,
                    new String[] { "Id" },
                    new Object[] { TEMPLATE_ID },
                    getEntityList());
        }
    }

    @Test
    public void testRemoveCantDo() throws Exception {
        doTestBadRemove(false, true, CANT_DO);
    }

    @Test
    public void testRemoveFailed() throws Exception {
        doTestBadRemove(true, false, FAILURE);
    }

    protected void doTestBadRemove(boolean valid, boolean success, String detail) throws Exception {
        setUpGetEntityExpectations(1);
        setUriInfo(setUpActionExpectations(ActionType.RemoveDisk,
                RemoveDiskParameters.class,
                new String[] { "DiskId" },
                new Object[] { GUIDS[1] },
                valid,
                success));
        try {
            resource.remove();
            fail("expected WebApplicationException");
        } catch (WebApplicationException wae) {
            verifyFault(wae, detail);
        }
    }

    protected void testCopyBySdName(boolean isFiltered) throws Exception {
        setUriInfo(setUpBasicUriExpectations());

        if (isFiltered) {
            setUpFilteredQueryExpectations();
            setUpEntityQueryExpectations(QueryType.GetAllStorageDomains,
                    QueryParametersBase.class,
                    new String[] {},
                    new Object[] {},
                    Collections.singletonList(getStorageDomainEntity()));
        }
        else {
            setUpEntityQueryExpectations(QueryType.GetStorageDomainByName,
                    NameQueryParameters.class,
                    new String[] { "Name" },
                    new Object[] { NAMES[2] },
                    getStorageDomainStaticEntity());
        }
        setUpEntityQueryExpectations(QueryType.GetDiskByDiskId,
                IdQueryParameters.class,
                new String[] { "Id" },
                new Object[] { GUIDS[1] },
                getEntity(3));
        setUriInfo(setUpActionExpectations(ActionType.MoveOrCopyDisk,
                MoveOrCopyImageGroupParameters.class,
                new String[] { "ImageId", "ImageGroupID", "SourceDomainId", "StorageDomainId", "Operation" },
                new Object[] { GUIDS[1], GUIDS[3], Guid.Empty, GUIDS[3], ImageOperation.Copy }));

        verifyActionResponse(resource.copy(setUpCopyParams(true)));
    }

    protected org.ovirt.engine.core.common.businessentities.StorageDomain getStorageDomainEntity() {
        org.ovirt.engine.core.common.businessentities.StorageDomain entity = mock(org.ovirt.engine.core.common.businessentities.StorageDomain.class);
        return setUpStorageDomainEntityExpectations(entity, StorageType.NFS);
    }

    protected org.ovirt.engine.core.common.businessentities.StorageDomainStatic getStorageDomainStaticEntity() {
        org.ovirt.engine.core.common.businessentities.StorageDomainStatic entity =
                mock(org.ovirt.engine.core.common.businessentities.StorageDomainStatic.class);
        return setUpStorageDomainEntityExpectations(entity, StorageType.NFS);
    }

    static org.ovirt.engine.core.common.businessentities.StorageDomainStatic setUpStorageDomainEntityExpectations(org.ovirt.engine.core.common.businessentities.StorageDomainStatic entity,
            StorageType storageType) {
        when(entity.getId()).thenReturn(GUIDS[3]);
        when(entity.getStorageName()).thenReturn(NAMES[2]);
        when(entity.getStorageDomainType()).thenReturn(StorageDomainType.Master);
        when(entity.getStorageType()).thenReturn(storageType);
        when(entity.getStorage()).thenReturn(GUIDS[0].toString());
        return entity;
    }

    static org.ovirt.engine.core.common.businessentities.StorageDomain setUpStorageDomainEntityExpectations(org.ovirt.engine.core.common.businessentities.StorageDomain entity, StorageType storageType) {
        when(entity.getId()).thenReturn(GUIDS[3]);
        when(entity.getStorageName()).thenReturn(NAMES[2]);
        when(entity.getStatus()).thenReturn(StorageDomainStatus.Active);
        when(entity.getStorageDomainType()).thenReturn(StorageDomainType.Master);
        when(entity.getStorageType()).thenReturn(storageType);
        when(entity.getStorage()).thenReturn(GUIDS[0].toString());
        return entity;
    }
    private Action setUpCopyParams(boolean byName) {
        Action action = new Action();
        StorageDomain sd = new StorageDomain();
        if (byName) {
            sd.setName(NAMES[2]);
        } else {
            sd.setId(GUIDS[3].toString());
        }
        action.setStorageDomain(sd);
        return action;
    }

    private void verifyActionResponse(Response r) throws Exception {
        verifyActionResponse(r, "templates/" + TEMPLATE_ID + "/disks/" + DISK_ID, false);
    }

    @Test
    public void testIncompleteCopy() throws Exception {
        setUriInfo(setUpBasicUriExpectations());
        try {
            resource.copy(new Action());
            fail("expected WebApplicationException on incomplete parameters");
        } catch (WebApplicationException wae) {
             verifyIncompleteException(wae, "Action", "copy", "storageDomain.id|name");
        }
    }

    protected UriInfo setUpActionExpectations(ActionType task,
            Class<? extends ActionParametersBase> clz,
            String[] names,
            Object[] values) {
        return setUpActionExpectations(task, clz, names, values, true, true, null, null, true);
    }

    protected void setUpFilteredQueryExpectations() {
        List<String> filterValue = new ArrayList<>();
        filterValue.add("true");
        reset(httpHeaders);
        when(httpHeaders.getRequestHeader(USER_FILTER_HEADER)).thenReturn(filterValue);
    }

    private org.ovirt.engine.core.common.businessentities.storage.Disk setUpEntityExpectations(DiskImage entity, int index) {
        when(entity.getId()).thenReturn(GUIDS[index]);
        when(entity.getVmSnapshotId()).thenReturn(GUIDS[2]);
        when(entity.getVolumeFormat()).thenReturn(VolumeFormat.RAW);
        when(entity.getImageStatus()).thenReturn(ImageStatus.OK);
        when(entity.getVolumeType()).thenReturn(VolumeType.Sparse);
        when(entity.isShareable()).thenReturn(false);
        when(entity.getPropagateErrors()).thenReturn(PropagateErrors.On);
        when(entity.getDiskStorageType()).thenReturn(DiskStorageType.IMAGE);
        when(entity.getImageId()).thenReturn(GUIDS[1]);
        ArrayList<Guid> sdIds = new ArrayList<>();
        sdIds.add(Guid.Empty);
        when(entity.getStorageIds()).thenReturn(sdIds);
        return setUpStatisticalEntityExpectations(entity);
    }

    private org.ovirt.engine.core.common.businessentities.storage.Disk setUpStatisticalEntityExpectations(DiskImage entity) {
        when(entity.getReadRate()).thenReturn(1);
        when(entity.getWriteRate()).thenReturn(2);
        when(entity.getReadLatency()).thenReturn(3.0);
        when(entity.getWriteLatency()).thenReturn(4.0);
        when(entity.getFlushLatency()).thenReturn(5.0);
        return entity;
    }

    private void verifyModelSpecific(Disk model, int index) {
        assertEquals(GUIDS[index].toString(), model.getId());
        assertTrue(model.isSparse());
        assertTrue(model.isPropagateErrors());
    }

}
