const STATIC_INVOICE_CONFIG = {
  formConfig: {
    extraFormFields: [
      // ... existing extraFormFields
    ],
    formFields: [
      {
        "name": "acc_id",
        "label": "Account Name",
        // ... existing config
      },
      // ... other existing fields
      {
        "name": "attach",
        "label": "Attach Invoice",
        "placeholder": "Drag & drop or click to upload",
        "type": "file",
        "mandatory": true,
        "disabled": false,
        "hidden": false,
        "readOnly": false,
        "pattern": null,
        "patternMessage": "",
        "defaultValue": "",
        "className": "",
        "fileFormat": "pdf",  // New: file extension
        "fileSize": "10MB"    // New: max file size
      },
      {
        "name": "comments",
        "label": "Notes",
        // ... existing config
      }
    ]
  },
  defaultDateRange: {
    dateFilterDefaultRange: '7',
  },
};
case 'file':
    return (
        <FormField
            key={fieldName}
            control={form.control}
            name={fieldName}
            rules={rules}
            render={({ field }) => (
                <FormItem>
                    <FormLabel className={`gap-1 font-medium ${config.className}`}>
                        {config.label}
                        {config.mandatory && <span className="text-red-500">*</span>}
                    </FormLabel>
                    <div>
                        <div
                            className="border-2 border-dashed border-input rounded-lg p-3 sm:p-4 flex flex-col items-center justify-center bg-transparent hover:bg-gray-100 dark:hover:bg-gray-900 transition h-[100px] sm:h-[115px] cursor-pointer relative shadow-none"
                            onClick={() => document.getElementById(`${fieldName}_input`)?.click()}
                        >
                            <input
                                type="file"
                                id={`${fieldName}_input`}
                                accept={`.${config.fileFormat}` || '.pdf'}
                                className="hidden"
                                disabled={config.disabled || config.readOnly}
                                onChange={async (e) => {
                                    const file = e.target.files?.[0];
                                    if (!file) return;

                                    // File size check
                                    const sizeStr = config.fileSize || '';
                                    let maxSize = 0;
                                    if (sizeStr) {
                                        const match = sizeStr.match(/(\d+)\s*(kb|mb|b)/i);
                                        if (match) {
                                            const num = parseInt(match[1], 10);
                                            const unit = match[2].toLowerCase();
                                            if (unit === 'kb') maxSize = num * 1024;
                                            else if (unit === 'mb') maxSize = num * 1024 * 1024;
                                            else if (unit === 'b') maxSize = num;
                                        }
                                    }
                                    if (maxSize > 0 && file.size > maxSize) {
                                        toast.warning(`File size exceeds maximum allowed (${sizeStr})`);
                                        e.target.value = '';
                                        return;
                                    }

                                    // File format check
                                    const allowedExt = config.fileFormat?.replace('.', '').trim().toLowerCase() || 'pdf';
                                    const fileExt = file.name.split('.').pop()?.toLowerCase();

                                    if (fileExt !== allowedExt) {
                                        toast.warning(`Only ${allowedExt.toUpperCase()} files are allowed.`);
                                        e.target.value = '';
                                        return;
                                    }

                                    const reader = new FileReader();
                                    reader.onloadend = () => {
                                        const base64Content = reader.result.split(',')[1];

                                        try {
                                            const blob = base64ToBlob(base64Content, file.type);
                                            if (blob.size === 0) {
                                                throw new Error('File conversion failed: Empty Blob');
                                            }

                                            const testUrl = URL.createObjectURL(blob);
                                            form.setValue('invoiceFile', file);
                                            form.setValue('attach', base64Content, { shouldValidate: true });
                                            form.setValue('att_name', file.name);
                                            form.setValue('att_size', Math.round(file.size / 1024).toString());
                                            
                                            if (editingRow && editingRow.att_id) {
                                                form.setValue('att_id', editingRow.att_id);
                                            }

                                            URL.revokeObjectURL(testUrl);
                                        } catch (error) {
                                            console.error('File processing error:', error);
                                            toast.error('The file could not be processed correctly.');
                                        }
                                    };

                                    reader.readAsDataURL(file);
                                }}
                            />

                            {form.watch('attach') || form.watch('att_id') ? (
                                <div className="flex flex-col items-center justify-center w-full space-y-1 min-w-0">
                                    {(form.watch('invoiceFile')?.name || form.watch('att_name')) && (
                                        <div className="flex flex-col items-center text-center">
                                            <span
                                                className="text-xs sm:text-sm font-semibold text-blue-600 truncate block max-w-[300px]"
                                                title={form.watch('invoiceFile')?.name || form.watch('att_name')}
                                            >
                                                {form.watch('invoiceFile')?.name || form.watch('att_name')}
                                            </span>

                                            {(form.watch('invoiceFile') || form.watch('att_size')) && (
                                                <span className="text-[10px] sm:text-xs text-gray-500">
                                                    (
                                                    {form.watch('invoiceFile')
                                                        ? (form.watch('invoiceFile').size / 1024).toFixed(0)
                                                        : form.watch('att_size')}{' '}
                                                    KB)
                                                </span>
                                            )}
                                        </div>
                                    )}

                                    <div className="flex gap-2 w-full max-w-[200px]">
                                        {editingRow && form.watch('att_id') && !form.watch('invoiceFile') && (
                                            <Button
                                                type="button"
                                                variant="outline"
                                                size="sm"
                                                className="h-7 flex-1 text-[10px]"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    downloadInvoice(editingRow);
                                                }}
                                            >
                                                <Download className="mr-1" size={12} /> Download
                                            </Button>
                                        )}

                                        <Button
                                            type="button"
                                            variant="destructive"
                                            size="sm"
                                            className="h-7 flex-1 text-[10px]"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                form.setValue('attach', '', { shouldValidate: true });
                                                form.setValue('invoiceFile', null);
                                                form.setValue('att_name', '');
                                                form.setValue('att_size', '');
                                                form.setValue('att_url', '');
                                                form.setValue('att_id', '');
                                            }}
                                        >
                                            Remove
                                        </Button>
                                    </div>
                                </div>
                            ) : (
                                <div className="flex flex-col items-center text-center">
                                    <span className="text-gray-400 text-xs sm:text-sm">{config.placeholder}</span>
                                    <span className="text-[9px] sm:text-[10px] text-gray-400 uppercase mt-1">
                                        {config.fileFormat?.toUpperCase()} Max {config.fileSize}
                                    </span>
                                </div>
                            )}
                        </div>
                        <div className="min-h-[20px]">
                            <FormMessage />
                        </div>
                    </div>
                </FormItem>
            )}
        />
    );

												<form autoComplete="off" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-4 sm:gap-x-6 p-2 sm:p-4 invoice-form">
    {/* Render main fields dynamically based on order */}
    {mainFields.map((fieldName) => {
        return <div key={fieldName}>{renderField(fieldName)}</div>;
    })}

    {/* Syndicate section - conditionally rendered */}
    {watchedStatus === 'syndicate' && syndicateFields.length > 0 && (
        <div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 gap-0 mt-2 sm:mt-6">
            <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-2 sm:p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 mb-4">
                {syndicateFields.map((fieldName) => renderField(fieldName))}
            </div>
        </div>
    )}

    {/* File and Comments section - dynamic from config */}
    <div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 lg:grid-cols-2 gap-x-4 sm:gap-x-6">
        {['attach', 'comments'].map((fieldName) => {
            const config = getConfig(fieldName);
            if (config.hidden) return null;
            return <div key={fieldName}>{renderField(fieldName)}</div>;
        })}
    </div>
</form>


		const rules = {};
if (config.mandatory && !config.readOnly && !config.disabled) {
    if (config.type === 'file') {
        rules.validate = () => (form.getValues().attach || form.getValues().att_id ? true : `${config.label} is required`);
    } else {
        rules.required = `${config.label} is required`;
    }
}


																				



		  
