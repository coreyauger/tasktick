import * as React from 'react';
import {observer} from 'mobx-react';
import { Theme } from '@material-ui/core/styles/createMuiTheme';
import createStyles from '@material-ui/core/styles/createStyles';
import withStyles, { WithStyles } from '@material-ui/core/styles/withStyles';
import Paper from '@material-ui/core/Paper';
import Checkbox from '@material-ui/core/Checkbox';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableFooter from '@material-ui/core/TableFooter';
import TablePagination from '@material-ui/core/TablePagination';
import TableRow from '@material-ui/core/TableRow';
import TableHead from '@material-ui/core/TableHead';
import withRoot from '../withRoot';
import OutIcon from '@material-ui/icons/ArrowLeft';
import InIcon from '@material-ui/icons/ExitToApp';
import ErrorIcon from '@material-ui/icons/Error';
import TablePager from './TablePager';


const styles = (theme: Theme) =>
  createStyles({
    root: {
      width: '100%'      
    },
    table: {
      minWidth: 500,
    },
    tableWrapper: {
      overflowX: 'auto',
    },
  });

type State = {
  page: number,
  rowsPerPage: number; 
  selectedIds: string[];  
};

interface Props {
  store: any;  
  multiSelect?: boolean;
  service?: string
  onSelect?: (Trace) => void;
};

@observer
class TraceTable extends React.Component<Props & WithStyles<typeof styles>, State> {
  state = {
    page: 0,
    rowsPerPage: 25,
    selectedIds: []
  };

  handleChangePage = (event, page) => {
    this.setState({...this.state, page });
  };

  handleChangeRowsPerPage = event => {
    this.setState({...this.state, rowsPerPage: event.target.value });
  };
  handleClick = (event, selectedId:string) => {
    if(this.props.multiSelect){
      if(this.isInSelected(selectedId))
        this.setState({...this.state, selectedIds: this.state.selectedIds.filter(x => x != selectedId) });
      else
        this.setState({...this.state, selectedIds: [...this.state.selectedIds, selectedId] });
    }else{
      this.setState({...this.state, selectedIds: [selectedId] });
    }
    if(this.props.onSelect)this.props.onSelect(this.props.store.traceStore.events[selectedId])
  }
  isSelected = (id: string): boolean => 
    this.state.selectedIds[this.state.selectedIds.length -1 ] == id  
  isInSelected = (id: string): boolean => 
  this.state.selectedIds.indexOf(id) != -1  

  render() {    
    const traceStream = this.props.store.traceStore.stream    
    const  classes = this.props.classes;
   
    //const rows =  (this.props.service ? traceStream.filter( (x:Trace) => x.service == this.props.service ) : traceStream).map( x => ({id: x.id, dir: x.dir, name: x.event.meta.eventType, service: x.service, time: x.event.meta.occurredAt}));
    const rows =  []
    const emptyRows = this.state.rowsPerPage - Math.min(this.state.rowsPerPage, rows.length - this.state.page * this.state.rowsPerPage);    
    return (<Paper className={classes.root}>
      <div className={classes.tableWrapper}>
        <Table className={classes.table}>
        <TableHead>
          <TableRow>
            {this.props.multiSelect && <TableCell></TableCell>}
            <TableCell></TableCell>
            <TableCell>Event Type</TableCell>
            <TableCell>Service</TableCell>
            <TableCell>Time</TableCell>        
          </TableRow>
        </TableHead>
          <TableBody>
            {rows.slice(this.state.page * this.state.rowsPerPage, this.state.page * this.state.rowsPerPage + this.state.rowsPerPage).map(row => {
              return (
                <TableRow key={row.id}
                  onClick={event => this.handleClick(event, row.id)}
                  selected={this.isSelected(row.id)}
                >
                 {this.props.multiSelect && <TableCell padding="checkbox">
                      <Checkbox checked={this.isInSelected(row.id)} />
                  </TableCell>}
                  <TableCell>{ (row.dir == "in" ? <InIcon /> : row.dir == "out" ? <OutIcon /> : <ErrorIcon /> ) }</TableCell>
                  <TableCell component="th" scope="row">
                    {row.name}
                  </TableCell>
                  <TableCell>{row.service}</TableCell>
                  <TableCell>{row.time}</TableCell>
                </TableRow>
              );
            })}
            {emptyRows > 0 && (
              <TableRow style={{ height: 48 * emptyRows }}>
                <TableCell colSpan={6} />
              </TableRow>
            )}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TablePagination
                rowsPerPageOptions={[25, 50, 100, 250]}
                colSpan={3}
                count={rows.length}
                rowsPerPage={this.state.rowsPerPage}
                page={this.state.page}
                onChangePage={this.handleChangePage}
                onChangeRowsPerPage={this.handleChangeRowsPerPage}
                ActionsComponent={TablePager}
              />
            </TableRow>
          </TableFooter>
        </Table>
      </div>
    </Paper>);       
  }
}

export default withRoot(withStyles(styles)(TraceTable));