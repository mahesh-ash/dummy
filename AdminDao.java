// Field component mapping
//Add this after your existing getConfig function (around line 1350):

const renderField = (fieldName, fieldConfig) => {
  const config = getConfig(fieldName);
  
  switch (fieldName) {
    case 'acc_id':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="acc_id"
          rules={{
            required: config.mandatory ? 'Account Name is required' : false,
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium">
                Account Name
                {config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div style={{ position: 'relative' }}>
                <AsyncTypeahead
                  id="acc_id"
                  labelKey="account_name"
                  options={accountOptions}
                  isLoading={isLoading}
                  minLength={1}
                  placeholder="Search Account Name"
                  onSearch={handleAccountSearch}
                  onChange={handleAccountChange}
                  selected={selectedAccount}
                  disabled={config.disabled || !!editingRow}
                  inputProps={{
                    className: 'default-input !h-[40px] dark:placeholder-grey-100',
                    style: { pointerEvents: 'auto' },
                    required: config.mandatory,
                  }}
                  menuStyle={{
                    zIndex: 1050,
                    width: '100%',
                    position: 'absolute',
                  }}
                  renderMenu={renderAccountMenu}
                />
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'invoice_date':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="invoice_date"
          rules={{
            required: config.mandatory ? 'Invoice Date is required' : false,
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className={`gap-1 font-medium ${config.disabled ? 'opacity-50' : ''}`}>
                Invoice Date
                {config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <Popover open={open} onOpenChange={setOpen}>
                  <PopoverTrigger asChild>
                    <Button
                      variant="outline"
                      disabled={config.disabled}
                      className={
                        'w-full justify-start text-left h-10 ' +
                        'flex rounded-md border border-input bg-background px-4 py-2 ring-offset-background ' +
                        'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 ' +
                        (field.value ? '' : ' text-muted-foreground') +
                        (config.disabled ? ' opacity-50 cursor-not-allowed bg-blue-100' : '')
                      }
                    >
                      <CalendarIcon className="mr-2 h-4 w-4" />
                      {field.value ? formatDateTime(field.value) : <span>Pick a date</span>}
                    </Button>
                  </PopoverTrigger>
                  {!config.disabled && (
                    <PopoverContent className="w-auto p-0">
                      <Calendar
                        mode="single"
                        selected={field.value ? field.value : undefined}
                        onSelect={(date) => {
                          if (!date) {
                            field.onChange('');
                            return;
                          }
                          const formattedDate = format(date, 'yyyy-MM-dd');
                          field.onChange(formattedDate);
                          setOpen(false);
                        }}
                        disabled={isFutureDate}
                        initialFocus
                      />
                    </PopoverContent>
                  )}
                </Popover>
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'invoice_no':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="invoice_no"
          rules={{
            required: config.mandatory ? 'Invoice Number is required' : false,
            min: {
              value: 0,
              message: 'invoice number cannot be negative',
            },
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium">
                Invoice Number
                {config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <Input
                  {...field}
                  type="text"
                  disabled={config.disabled || !!editingRow}
                  maxLength={256}
                  className=" disabled:bg-blue-100 dark:disabled:bg-gray-900"
                  placeholder="Enter invoice number"
                  required={config.mandatory}
                />
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'invoice_status':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="invoice_status"
          rules={{
            required: config.mandatory ? 'Invoice Status is required' : false,
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium">
                Invoice Status
                {config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={config.disabled}
                  required={config.mandatory}
                >
                  <SelectTrigger className="w-full h-10 justify-between dark:disabled:bg-gray-900 disabled:bg-blue-100  dark:bg-inherit text-sm">
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    {config.options.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'commission':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="commission"
          rules={{
            required: config.mandatory ? 'Commission is required' : false,
            min: {
              value: 0,
              message: 'commission number cannot be negative',
            },
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium">
                Commission
                {config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <Input
                  {...field}
                  type="number"
                  onBlur={(e) => {
                    field.onBlur();
                    if (e.target.value !== '') {
                      field.onChange(parseFloat(e.target.value).toFixed(2));
                    }
                  }}
                  disabled={config.disabled}
                  className=" dark:disabled:bg-gray-900 disabled:bg-blue-100"
                  placeholder="Enter commission amount"
                  required={config.mandatory}
                />
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'product':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="product"
          rules={{
            required: config.mandatory ? 'Product is required' : false,
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium">
                Product
                {config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <Select value={field.value} onValueChange={field.onChange} disabled={config.disabled || !!editingRow}>
                  <SelectTrigger className="w-full disabled:cursor-not-allowed justify-between h-10 dark:bg-inherit text-sm disabled:bg-blue-100 dark:disabled:bg-gray-900">
                    <SelectValue placeholder="Select product" />
                  </SelectTrigger>
                  <SelectContent>
                    {config.options.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'ticker':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="ticker"
          rules={{
            required: config.mandatory ? 'Ticker is required' : false,
            min: {
              value: 0,
              message: 'Ticker cannot be negative',
            },
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium text-sm">
                Ticker{config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <AsyncTypeahead
                  id="ticker"
                  labelKey={(option) => option.ticker_symbol}
                  options={tickerOptions}
                  isLoading={isTickerLoading}
                  minLength={1}
                  placeholder="Search Ticker..."
                  onSearch={handleTickerSearch}
                  onChange={handleTickerChange}
                  selected={selectedTicker}
                  disabled={config.disabled}
                  inputProps={{
                    className:
                      'flex h-10 w-full rounded-md border border-input bg-background px-4 py-2 text-sm sm:text-base ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:bg-blue-100 disabled:opacity-50',
                    maxLength: 32,
                  }}
                  menuStyle={{
                    zIndex: 1050,
                    width: '100%',
                    position: 'absolute',
                  }}
                  renderMenu={renderTickerMenu}
                />
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'quantity':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="quantity"
          rules={{
            required: config.mandatory ? 'Quantity is required' : false,
            min: {
              value: 0,
              message: 'Quantity cannot be negative',
            },
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium text-sm">
                Quantity{config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <Input
                  {...field}
                  disabled={config.disabled}
                  type="number"
                  onBlur={(e) => {
                    field.onBlur();
                    if (e.target.value !== '') {
                      field.onChange(parseFloat(e.target.value).toFixed(2));
                    }
                  }}
                  min={0}
                  className=" disabled:bg-blue-100 dark:disabled:bg-gray-900"
                  placeholder="Quantity"
                />
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'price':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="price"
          rules={{
            required: config.mandatory ? 'Price is required' : false,
            min: {
              value: 0,
              message: 'Price cannot be negative',
            },
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium text-sm">
                Price{config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <Input
                  {...field}
                  disabled={config.disabled}
                  type="number"
                  onBlur={(e) => {
                    field.onBlur();
                    if (e.target.value !== '') {
                      field.onChange(parseFloat(e.target.value).toFixed(2));
                    }
                  }}
                  min={0}
                  step="any"
                  className=" disabled:bg-blue-100 dark:disabled:bg-gray-900"
                  placeholder="Price"
                />
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'net_amount':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="net_amount"
          rules={{
            required: config.mandatory ? 'Net Amount is required' : false,
            min: {
              value: 0,
              message: 'net Amount cannot be negative',
            },
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium text-sm">
                Net Amount{config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <Input
                  {...field}
                  disabled={config.disabled}
                  type="number"
                  onBlur={(e) => {
                    field.onBlur();
                    if (e.target.value !== '') {
                      field.onChange(parseFloat(e.target.value).toFixed(2));
                    }
                  }}
                  min={0}
                  step="any"
                  className=" disabled:bg-blue-100 dark:disabled:bg-gray-900"
                  placeholder="Net Amount"
                />
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    case 'comments':
      return (
        <FormField
          key={fieldName}
          control={form.control}
          name="comments"
          rules={{
            required: config.mandatory ? 'Notes are required' : false,
          }}
          render={({ field }) => (
            <FormItem>
              <FormLabel className="gap-1 font-medium">
                Notes
                {config.mandatory && <span className="text-red-500">*</span>}
              </FormLabel>
              <div>
                <Textarea
                  {...field}
                  id="notes"
                  rows={4}
                  disabled={config.disabled}
                  className="shadow-none max-h-[150px] disabled:bg-blue-100 min-h-[115px] dark:disabled:bg-gray-900"
                  placeholder="Enter notes here..."
                  required={config.mandatory}
                ></Textarea>
                <div className="min-h-[20px]">
                  <FormMessage />
                </div>
              </div>
            </FormItem>
          )}
        />
      );

    default:
      return null;
  }
};

// Separate syndicate fields from main fields
const syndicateFields = ['ticker', 'quantity', 'price', 'net_amount'];
const mainFields = fieldOrder.filter(f => !syndicateFields.includes(f));

----------------------------------------------------------------------------------------------------------
    <form autoComplete="off" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-4 sm:gap-x-6 p-2 sm:p-4 invoice-form">
  {/* Render main fields dynamically based on order */}
  {mainFields.map(fieldName => {
    const config = getConfig(fieldName);
    if (config.hidden) return null;
    return <div key={fieldName}>{renderField(fieldName, config)}</div>;
  })}

  {/* Syndicate section - conditionally rendered */}
  {watchedStatus === 'syndicate' && (
    <div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 gap-0 mt-2 sm:mt-6">
      <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-2 sm:p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 mb-4">
        {syndicateFields.map(fieldName => {
          const config = getConfig(fieldName);
          if (config.hidden) return null;
          return renderField(fieldName, config);
        })}
      </div>
    </div>
  )}

  {/* Attachment and Comments section */}
  <div className="col-span-1 sm:col-span-2 lg:col-span-3 grid grid-cols-1 lg:grid-cols-2 gap-x-4 sm:gap-x-6">
    <FormField
      control={form.control}
      rules={{
        validate: () => (form.getValues().attach || form.getValues().att_id ? true : 'Invoice attachment is required'),
      }}
      name="attach"
      render={({ field }) => (
        <FormItem>
          <FormLabel className="gap-1 font-medium">
            Attach Invoice<span className="text-red-500">*</span>
          </FormLabel>
          <div>
            <div
              className="border-2 border-dashed border-input rounded-lg p-3 sm:p-4 flex flex-col items-center justify-center bg-transparent hover:bg-gray-100 dark:hover:bg-gray-900 transition h-[100px] sm:h-[115px] cursor-pointer relative shadow-none"
              onClick={() => document.getElementById('att_id')?.click()}
            >
              <input
                type="file"
                id="att_id"
                accept={`.${INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileFormat}` || '.pdf'}
                className="hidden"
                onChange={async (e) => {
                  const file = e.target.files?.[0];
                  if (!file) return;

                  const sizeStr = INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileSize || '';
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

                  const configFormat = INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileFormat || 'pdf';
                  const allowedExt = configFormat.replace('.', '').trim().toLowerCase();
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
                      console.error('Decryption Error:', error);
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
                  <span className="text-gray-400 text-xs sm:text-sm">Drag & drop or click to upload</span>
                  <span className="text-[9px] sm:text-[10px] text-gray-400 uppercase mt-1">
                    {INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileFormat} Max {INVOICE_IMAGE_VARIABLE.value.fileUpload?.fileSize}
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

    {!getConfig('comments').hidden && renderField('comments', getConfig('comments'))}
  </div>
</form>

