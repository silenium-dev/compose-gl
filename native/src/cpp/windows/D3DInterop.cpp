//
// Created by silenium-dev on 2025-10-04.
//

#include <gpu/ganesh/d3d/GrD3DBackendContext.h>
#include <gpu/ganesh/d3d/GrD3DTypes.h>

#define SK_DIRECT3D // for some reason, the d3d headers undefine this macro
#include <gpu/ganesh/GrBackendSurface.h>
#include <gpu/ganesh/GrDirectContext.h>

#include <comdef.h>
#include <d3d12.h>
#include <dcomp.h>
#include <dxgi1_4.h>
#include <iostream>
#include <jni.h>
#include <windows.h>
#include <wrl.h>

constexpr int BuffersCount = 2;

class DirectXDevice {
public:
    HWND hWnd;// Handle of native view.
    GrD3DBackendContext backendContext;
    gr_cp<ID3D12Device> device;
    gr_cp<IDXGISwapChain3> swapChain;
    gr_cp<ID3D12CommandQueue> queue;
    gr_cp<ID3D12Resource> buffers[BuffersCount];
    gr_cp<ID3D12Fence> fence;
    gr_cp<IDCompositionDevice> dcDevice;
    gr_cp<IDCompositionTarget> dcTarget;
    gr_cp<IDCompositionVisual> dcVisual;
    uint64_t fenceValues[BuffersCount];
    HANDLE fenceEvent = nullptr;
    unsigned int bufferIndex;
};

extern "C" {
JNIEXPORT jlong JNICALL Java_dev_silenium_compose_gl_interop_D3DInteropKt_createD3DTextureN(JNIEnv *env, jobject thiz, const jlong _device, const jint width, const jint height) {
    const auto device = reinterpret_cast<DirectXDevice *>(_device);

    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    desc.Width = width;
    desc.Height = height;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = 1;
    desc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    desc.SampleDesc.Count = 1;
    desc.SampleDesc.Quality = 0;
    desc.Layout = D3D12_TEXTURE_LAYOUT_UNKNOWN;
    desc.Flags = D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET;

    D3D12_HEAP_PROPERTIES heap{};
    heap.Type = D3D12_HEAP_TYPE_DEFAULT;
    heap.CPUPageProperty = D3D12_CPU_PAGE_PROPERTY_UNKNOWN;
    heap.MemoryPoolPreference = D3D12_MEMORY_POOL_UNKNOWN;
    heap.CreationNodeMask = 1;
    heap.VisibleNodeMask = 1;

    D3D12_CLEAR_VALUE clearValue{};
    clearValue.Format = desc.Format;
    clearValue.Color[0] = 1;
    clearValue.Color[1] = 0;
    clearValue.Color[2] = 0;
    clearValue.Color[3] = 1;

    ID3D12Resource *resource;
    const auto res = device->device->CreateCommittedResource(&heap, D3D12_HEAP_FLAG_SHARED, &desc, D3D12_RESOURCE_STATE_COMMON, &clearValue, IID_PPV_ARGS(&resource));
    if (FAILED(res)) {
        const _com_error err{res};
        std::cerr << "Failed to create resource: " << err.ErrorMessage() << std::endl;
        return 0;
    }

    return reinterpret_cast<jlong>(resource);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_gl_interop_D3DInteropKt_exportSharedHandleN(JNIEnv *env, jobject thiz, const jlong _device, const jlong _resource) {
    const auto resource = reinterpret_cast<ID3D12Resource *>(_resource);
    const auto device = reinterpret_cast<DirectXDevice *>(_device);

    HANDLE sharedHandle;
    const auto res = device->device->CreateSharedHandle(resource, nullptr, GENERIC_ALL, nullptr, &sharedHandle);
    if (FAILED(res)) {
        const _com_error err{res};
        std::cerr << "Failed to create shared handle: " << err.ErrorMessage() << std::endl;
        return 0;
    }
    return reinterpret_cast<jlong>(sharedHandle);
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_gl_interop_D3DInteropKt_makeD3DBackendTextureN(JNIEnv *env, jobject thiz, const jlong _resource) {
    const auto resource = reinterpret_cast<ID3D12Resource *>(_resource);
    const auto desc = resource->GetDesc();

    GrD3DTextureResourceInfo importInfo{};
    importInfo.fResource.retain(resource);
    importInfo.fLevelCount = 1;
    importInfo.fFormat = DXGI_FORMAT_R8G8B8A8_UNORM;

    const auto texture = new GrBackendTexture(static_cast<int>(desc.Width), static_cast<int>(desc.Height), importInfo);
    return reinterpret_cast<jlong>(texture);
}

JNIEXPORT void JNICALL Java_dev_silenium_compose_gl_interop_D3DInteropKt_destroyD3DTextureN(JNIEnv *env, jobject thiz, const jlong _resource) {
    const auto resource = reinterpret_cast<ID3D12Resource *>(_resource);
    resource->Release();
}

JNIEXPORT void JNICALL Java_dev_silenium_compose_gl_interop_D3DInteropKt_closeSharedHandleN(JNIEnv *env, jobject thiz, const jlong _handle) {
    const auto handle = reinterpret_cast<HANDLE>(_handle);
    CloseHandle(handle);
}
JNIEXPORT jstring JNICALL Java_dev_silenium_compose_gl_interop_D3DInteropKt_getDirectContextApiN(JNIEnv *env, jobject thiz, jobject _directContext) {
    const auto clazz = env->GetObjectClass(_directContext);
    const auto ptrField = env->GetFieldID(clazz, "_ptr", "J");
    const auto ptr = env->GetLongField(_directContext, ptrField);
    const auto directContext = reinterpret_cast<GrDirectContext *>(ptr);
    switch (directContext->backend()) {
        case GrBackendApi::kOpenGL:
            return env->NewStringUTF("kOpenGL");
        case GrBackendApi::kVulkan:
            return env->NewStringUTF("kVulkan");
        case GrBackendApi::kMetal:
            return env->NewStringUTF("kMetal");
        case GrBackendApi::kDirect3D:
            return env->NewStringUTF("kDirect3D");
        case GrBackendApi::kMock:
            return env->NewStringUTF("kMock");
        case GrBackendApi::kUnsupported:
            return env->NewStringUTF("kUnsupported");
        default:
            return nullptr;
    }
}
}
