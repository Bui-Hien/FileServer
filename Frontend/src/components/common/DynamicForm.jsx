import React from 'react';
import { TextField, MenuItem, Box } from '@mui/material';

const DynamicForm = ({ fields, formData, onChange }) => {
  if (!fields || fields.length === 0) return null;

  return (
    <Box>
      {fields.map((field) => {
        if (field.type === 'option') {
          return (
            <TextField
              key={field.name}
              select
              label={field.label}
              fullWidth
              sx={{ mt: 2 }}
              value={formData[field.name] || ''}
              onChange={(e) => onChange(field.name, e.target.value)}
            >
              {(field.options || []).map((opt) => (
                <MenuItem key={opt.value} value={opt.value}>{opt.label}</MenuItem>
              ))}
            </TextField>
          );
        }

        if (field.type === 'api') {
          return (
            <ApiSelect
              key={field.name}
              field={field}
              value={formData[field.name] || ''}
              onChange={(val) => onChange(field.name, val)}
            />
          );
        }

        return (
          <TextField
            key={field.name}
            label={field.label}
            type={field.type === 'number' || field.type === 'double' ? 'number' : 
                  field.type === 'date' ? 'date' : 
                  field.type === 'date_time' ? 'datetime-local' : 'text'}
            fullWidth
            sx={{ mt: 2 }}
            InputLabelProps={{ shrink: true }}
            multiline={field.type === 'textarea' || field.type === 'text'}
            rows={field.type === 'textarea' ? 3 : 1}
            value={formData[field.name] || ''}
            onChange={(e) => onChange(field.name, e.target.value)}
          />
        );
      })}
    </Box>
  );
};

const ApiSelect = ({ field, value, onChange }) => {
  const [data, setData] = React.useState([]);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    if (field.api) {
      import('../../services/api').then(api => {
        api.default.get(field.api)
          .then(res => {
            setData(res.data);
            setLoading(false);
          })
          .catch(err => {
            console.error("API error", err);
            setLoading(false);
          });
      });
    }
  }, [field.api]);

  return (
    <TextField
      select
      label={field.label}
      fullWidth
      sx={{ mt: 2 }}
      disabled={loading}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      helperText={loading ? 'Đang tải...' : ''}
    >
      {data.map((item) => (
        // Giả sử API trả về list object có id/fullName hoặc value/label
        <MenuItem key={item.id || item.value} value={item.id || item.value}>
          {item.fullName || item.name || item.label || item.id}
        </MenuItem>
      ))}
    </TextField>
  );
};

export default DynamicForm;
